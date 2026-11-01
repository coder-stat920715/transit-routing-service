package com.transit.routing.service;

import com.transit.routing.model.gtfs.Place;
import com.transit.routing.model.journey.Journey;
import com.transit.routing.model.journey.Leg;
import com.transit.routing.model.journey.RoutingQuery;
import com.transit.routing.model.journey.TransitLeg;
import com.transit.routing.model.journey.WalkLeg;
import com.transit.routing.model.raptor.Footpath;
import com.transit.routing.model.raptor.PatternStopRef;
import com.transit.routing.model.raptor.PatternTrip;
import com.transit.routing.model.raptor.RoutePattern;
import com.transit.routing.model.raptor.TransitNetwork;
import com.transit.routing.model.raptor.WalkingProfile;
import com.transit.routing.util.HaversineDistanceCalculator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RAPTOR (Delling, Pajor, Werneck 2012) earliest-arrival engine with Pareto output over
 * (arrival time, number of trips). Round k = journeys using at most k transit trips.
 */
public class RaptorRoutingEngineService {
    private static final int INF = Integer.MAX_VALUE;
    private static final double PLACE_MATCH_METERS = 100.0;

    private final TransitNetwork network;
    private final SpatialIndexService spatialIndex;
    private final RoutingStrategy strategy;
    private final WalkingProfile walking;

    public RaptorRoutingEngineService(TransitNetwork network, SpatialIndexService spatialIndex,
                                      RoutingStrategy strategy, WalkingProfile walking) {
        this.network = network;
        this.spatialIndex = spatialIndex;
        this.strategy = strategy;
        this.walking = walking;
    }

    // Labels used for journey reconstruction
    private record AccessLabel(double distanceMeters) {
    }

    private record TransitLabel(int patternIdx, int tripIdx, int boardPos, int alightPos) {
    }

    private record FootLabel(int fromStop, Footpath path) {
    }

    /** Pareto-optimal journeys, ordered by increasing number of trips and strictly decreasing arrival time. */
    public List<Journey> findJourneys(RoutingQuery q) {
        final int n = network.stopCount();
        final int maxRounds = Math.max(0, q.maxTransfers()) + 1;
        final int dep = q.departureSec();

        int[][] tau = new int[maxRounds + 1][n];
        int[][] transitArr = new int[maxRounds + 1][n];
        TransitLabel[][] transitLabel = new TransitLabel[maxRounds + 1][n];
        FootLabel[][] footLabel = new FootLabel[maxRounds + 1][n];
        boolean[][] viaFoot = new boolean[maxRounds + 1][n];
        AccessLabel[] access = new AccessLabel[n];
        int[] tauStar = new int[n];
        for (int k = 0; k <= maxRounds; k++) {
            Arrays.fill(tau[k], INF);
            Arrays.fill(transitArr[k], INF);
        }
        Arrays.fill(tauStar, INF);

        String originName = placeName(q.originLat(), q.originLon(), "Origin");
        String destName = placeName(q.destLat(), q.destLon(), "Destination");

        List<Journey> result = new ArrayList<>();
        int bestDest = INF;

        // Direct walk (round 0 candidate)
        double direct = HaversineDistanceCalculator.distanceMeters(q.originLat(), q.originLon(), q.destLat(), q.destLon());
        if (direct <= q.maxWalkMeters()) {
            int arr = dep + walking.walkSeconds(direct);
            bestDest = arr;
            result.add(new Journey(List.of(new WalkLeg(originName, destName, direct, dep, arr)), dep, arr, 0));
        }

        // Egress stops
        int[] egressSec = new int[n];
        double[] egressDist = new double[n];
        Arrays.fill(egressSec, -1);
        List<SpatialIndexService.StopDistance> egressStops = spatialIndex.findWithin(q.destLat(), q.destLon(), q.maxWalkMeters());
        for (SpatialIndexService.StopDistance sd : egressStops) {
            egressSec[sd.stopIdx()] = walking.walkSeconds(sd.distanceMeters());
            egressDist[sd.stopIdx()] = sd.distanceMeters();
        }

        // Round 0: access walk to stops near the origin
        List<Integer> marked = new ArrayList<>();
        for (SpatialIndexService.StopDistance sd : spatialIndex.findWithin(q.originLat(), q.originLon(), q.maxWalkMeters())) {
            int s = sd.stopIdx();
            int arr = dep + walking.walkSeconds(sd.distanceMeters());
            if (arr < tauStar[s] && arr < bestDest) {
                tau[0][s] = arr;
                tauStar[s] = arr;
                access[s] = new AccessLabel(sd.distanceMeters());
                marked.add(s);
            }
        }

        for (int k = 1; k <= maxRounds && !marked.isEmpty(); k++) {
            // Accumulate routes serving marked stops, remembering the earliest marked position on each
            Map<Integer, Integer> queue = new LinkedHashMap<>();
            for (int p : marked) {
                for (PatternStopRef ref : network.routesByStop().patternsAt(p)) {
                    queue.merge(ref.patternIdx(), ref.position(), Math::min);
                }
            }

            List<Integer> newMarked = new ArrayList<>();
            boolean[] isMarked = new boolean[n];

            // Traverse each queued route
            for (Map.Entry<Integer, Integer> qe : queue.entrySet()) {
                RoutePattern pat = network.patterns().get(qe.getKey());
                PatternTrip[] trips = pat.trips();
                int cur = -1;
                int boardPos = -1;
                for (int i = qe.getValue(); i < pat.size(); i++) {
                    int stop = pat.stopIdx()[i];
                    if (cur >= 0) {
                        int arr = strategy.arrivalSeconds(pat, trips[cur], i);
                        if (arr < Math.min(tauStar[stop], bestDest)) {
                            tau[k][stop] = arr;
                            transitArr[k][stop] = arr;
                            tauStar[stop] = arr;
                            transitLabel[k][stop] = new TransitLabel(pat.index(), cur, boardPos, i);
                            viaFoot[k][stop] = false;
                            if (!isMarked[stop]) {
                                isMarked[stop] = true;
                                newMarked.add(stop);
                            }
                        }
                    }
                    int prev = tau[k - 1][stop];
                    if (prev != INF) {
                        int curDep = cur >= 0 ? strategy.departureSeconds(pat, trips[cur], i) : INF;
                        if (prev <= curDep) {
                            int t = earliestTrip(pat, i, prev, cur >= 0 ? cur : trips.length);
                            if (t >= 0) {
                                cur = t;
                                boardPos = i;
                            }
                        }
                    }
                }
            }

            // Footpath relaxation (from stops improved by transit in this round only)
            List<Integer> transitImproved = new ArrayList<>(newMarked);
            for (int p : transitImproved) {
                for (Footpath fp : network.footpaths().get(p)) {
                    int q2 = fp.toStop();
                    int arr = transitArr[k][p] + fp.seconds();
                    if (arr < Math.min(tauStar[q2], bestDest)) {
                        tau[k][q2] = arr;
                        tauStar[q2] = arr;
                        footLabel[k][q2] = new FootLabel(p, fp);
                        viaFoot[k][q2] = true;
                        if (!isMarked[q2]) {
                            isMarked[q2] = true;
                            newMarked.add(q2);
                        }
                    }
                }
            }
            marked = newMarked;

            // Evaluate destination after round k
            int best = INF;
            int bestStop = -1;
            for (SpatialIndexService.StopDistance sd : egressStops) {
                int s = sd.stopIdx();
                if (tau[k][s] == INF) {
                    continue;
                }
                int total = tau[k][s] + egressSec[s];
                if (total < best) {
                    best = total;
                    bestStop = s;
                }
            }
            if (bestStop >= 0 && best < bestDest) {
                bestDest = best;
                List<Leg> legs = reconstruct(k, bestStop, best, dep, originName, destName, egressDist[bestStop],
                        tau, transitArr, transitLabel, footLabel, viaFoot, access);
                result.add(new Journey(legs, dep, best, k));
            }
        }
        return result;
    }

    /** Journey with the earliest arrival (fewest trips among ties). */
    public Optional<Journey> findBestJourney(RoutingQuery q) {
        List<Journey> all = findJourneys(q);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1));
    }

    /** Binary search: first trip index in [0, upperExclusive) departing at/after time at this position, or -1. */
    private int earliestTrip(RoutePattern pat, int pos, int time, int upperExclusive) {
        PatternTrip[] trips = pat.trips();
        int lo = 0;
        int hi = upperExclusive;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (strategy.departureSeconds(pat, trips[mid], pos) >= time) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo < upperExclusive ? lo : -1;
    }

    private List<Leg> reconstruct(int k, int lastStop, int finalArrival, int dep, String originName, String destName,
                                  double egressMeters, int[][] tau, int[][] transitArr,
                                  TransitLabel[][] transitLabel, FootLabel[][] footLabel,
                                  boolean[][] viaFoot, AccessLabel[] access) {
        Deque<Leg> legs = new ArrayDeque<>();
        legs.addFirst(new WalkLeg(network.stop(lastStop).name(), destName, egressMeters, tau[k][lastStop], finalArrival));
        int round = k;
        int cur = lastStop;
        while (true) {
            if (round == 0) {
                legs.addFirst(new WalkLeg(originName, network.stop(cur).name(), access[cur].distanceMeters(), dep, tau[0][cur]));
                break;
            }
            if (viaFoot[round][cur]) {
                FootLabel f = footLabel[round][cur];
                legs.addFirst(new WalkLeg(network.stop(f.fromStop()).name(), network.stop(cur).name(),
                        f.path().distanceMeters(), transitArr[round][f.fromStop()], tau[round][cur]));
                cur = f.fromStop();
            }
            TransitLabel t = transitLabel[round][cur];
            RoutePattern pat = network.patterns().get(t.patternIdx());
            PatternTrip trip = pat.trips()[t.tripIdx()];
            int boardStop = pat.stopIdx()[t.boardPos()];
            var route = network.routes().get(pat.routeId());
            legs.addFirst(new TransitLeg(pat.routeId(), route.shortName(), trip.headsign(), trip.tripId(),
                    network.stop(boardStop).name(), network.stop(cur).name(),
                    t.alightPos() - t.boardPos() - 1,
                    strategy.departureSeconds(pat, trip, t.boardPos()),
                    strategy.arrivalSeconds(pat, trip, t.alightPos())));
            cur = boardStop;
            round--;
        }
        return List.copyOf(legs);
    }

    private String placeName(double lat, double lon, String fallback) {
        return network.nearestPlace(lat, lon, PLACE_MATCH_METERS).map(Place::name).orElse(fallback);
    }
}
