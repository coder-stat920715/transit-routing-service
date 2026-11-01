package com.transit.routing.service;

import com.transit.routing.model.gtfs.GtfsRoute;
import com.transit.routing.model.gtfs.Place;
import com.transit.routing.model.gtfs.Stop;
import com.transit.routing.model.gtfs.StopTime;
import com.transit.routing.model.gtfs.Trip;
import com.transit.routing.model.raptor.Footpath;
import com.transit.routing.model.raptor.PatternStopRef;
import com.transit.routing.model.raptor.PatternTrip;
import com.transit.routing.model.raptor.RoutePattern;
import com.transit.routing.model.raptor.RoutesByStopIndex;
import com.transit.routing.model.raptor.TransitNetwork;
import com.transit.routing.model.raptor.TripStopTimesMap;
import com.transit.routing.model.raptor.WalkingProfile;
import com.transit.routing.util.CsvUtils;
import com.transit.routing.util.TimeUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Parses GTFS (stops, routes, trips, stop_times [+ optional places.txt]) into a {@link TransitNetwork}. */
public class GtfsLoaderService {
    private final WalkingProfile walking;
    private final double footpathRadiusMeters;

    public GtfsLoaderService(WalkingProfile walking, double footpathRadiusMeters) {
        this.walking = walking;
        this.footpathRadiusMeters = footpathRadiusMeters;
    }

    public TransitNetwork load(GtfsSource source) {
        try {
            List<Stop> stops = readStops(source);
            Map<String, Integer> stopIndex = new HashMap<>();
            for (int i = 0; i < stops.size(); i++) {
                if (stopIndex.put(stops.get(i).id(), i) != null) {
                    throw new GtfsLoadException("Duplicate stop_id: " + stops.get(i).id());
                }
            }
            Map<String, GtfsRoute> routes = readRoutes(source);
            Map<String, Trip> trips = readTrips(source, routes);
            Map<String, StopTime[]> stopTimes = readStopTimes(source, trips, stopIndex);

            List<RoutePattern> patterns = buildPatterns(trips, stopTimes, stopIndex);
            RoutesByStopIndex routesByStop = buildRoutesByStop(patterns, stops);
            List<List<Footpath>> footpaths = buildFootpaths(stops);
            List<Place> places = source.exists("places.txt") ? readPlaces(source) : List.of();

            return new TransitNetwork(List.copyOf(stops), Map.copyOf(stopIndex), Map.copyOf(routes),
                    List.copyOf(patterns), routesByStop, new TripStopTimesMap(stopTimes), footpaths, places);
        } catch (IOException e) {
            throw new GtfsLoadException("Failed to read GTFS feed", e);
        }
    }

    // ---- parsing ----------------------------------------------------------------------------

    private List<Stop> readStops(GtfsSource source) throws IOException {
        List<Stop> out = new ArrayList<>();
        for (Map<String, String> r : rows(source, "stops.txt")) {
            out.add(new Stop(req(r, "stop_id"), req(r, "stop_name"),
                    Double.parseDouble(req(r, "stop_lat")), Double.parseDouble(req(r, "stop_lon"))));
        }
        return out;
    }

    private Map<String, GtfsRoute> readRoutes(GtfsSource source) throws IOException {
        Map<String, GtfsRoute> out = new LinkedHashMap<>();
        for (Map<String, String> r : rows(source, "routes.txt")) {
            String type = r.getOrDefault("route_type", "3");
            out.put(req(r, "route_id"), new GtfsRoute(req(r, "route_id"),
                    r.getOrDefault("route_short_name", ""), r.getOrDefault("route_long_name", ""),
                    type.isBlank() ? 3 : Integer.parseInt(type)));
        }
        return out;
    }

    private Map<String, Trip> readTrips(GtfsSource source, Map<String, GtfsRoute> routes) throws IOException {
        Map<String, Trip> out = new LinkedHashMap<>();
        for (Map<String, String> r : rows(source, "trips.txt")) {
            String routeId = req(r, "route_id");
            if (!routes.containsKey(routeId)) {
                throw new GtfsLoadException("trips.txt references unknown route_id " + routeId);
            }
            out.put(req(r, "trip_id"), new Trip(req(r, "trip_id"), routeId,
                    r.getOrDefault("service_id", ""), r.getOrDefault("trip_headsign", "")));
        }
        return out;
    }

    private Map<String, StopTime[]> readStopTimes(GtfsSource source, Map<String, Trip> trips,
                                                   Map<String, Integer> stopIndex) throws IOException {
        Map<String, List<StopTime>> grouped = new LinkedHashMap<>();
        for (Map<String, String> r : rows(source, "stop_times.txt")) {
            String tripId = req(r, "trip_id");
            String stopId = req(r, "stop_id");
            if (!trips.containsKey(tripId)) {
                throw new GtfsLoadException("stop_times.txt references unknown trip_id " + tripId);
            }
            if (!stopIndex.containsKey(stopId)) {
                throw new GtfsLoadException("stop_times.txt references unknown stop_id " + stopId);
            }
            String arr = r.getOrDefault("arrival_time", "");
            String dep = r.getOrDefault("departure_time", "");
            if (arr.isBlank() && dep.isBlank()) {
                throw new GtfsLoadException("Missing arrival and departure time for trip " + tripId + " stop " + stopId);
            }
            int a = TimeUtils.parseGtfsTime(arr.isBlank() ? dep : arr);
            int d = TimeUtils.parseGtfsTime(dep.isBlank() ? arr : dep);
            grouped.computeIfAbsent(tripId, k -> new ArrayList<>())
                    .add(new StopTime(tripId, a, d, stopId, Integer.parseInt(req(r, "stop_sequence"))));
        }
        Map<String, StopTime[]> out = new LinkedHashMap<>();
        grouped.forEach((tripId, list) -> {
            list.sort(Comparator.comparingInt(StopTime::stopSequence));
            out.put(tripId, list.toArray(new StopTime[0]));
        });
        return out;
    }

    private List<Place> readPlaces(GtfsSource source) throws IOException {
        List<Place> out = new ArrayList<>();
        for (Map<String, String> r : rows(source, "places.txt")) {
            out.add(new Place(req(r, "place_name"),
                    Double.parseDouble(req(r, "place_lat")), Double.parseDouble(req(r, "place_lon"))));
        }
        return out;
    }

    // ---- index building ----------------------------------------------------------------------

    private List<RoutePattern> buildPatterns(Map<String, Trip> trips, Map<String, StopTime[]> stopTimes,
                                             Map<String, Integer> stopIndex) {
        Map<String, List<PatternTrip>> tripsByKey = new LinkedHashMap<>();
        Map<String, String> routeByKey = new HashMap<>();
        Map<String, String[]> stopsByKey = new HashMap<>();

        for (Trip trip : trips.values()) {
            StopTime[] st = stopTimes.get(trip.id());
            if (st == null || st.length < 2) {
                continue;
            }
            String[] stopIds = Arrays.stream(st).map(StopTime::stopId).toArray(String[]::new);
            String key = trip.routeId() + "|" + String.join(">", stopIds);
            int[] arr = new int[st.length];
            int[] dep = new int[st.length];
            for (int i = 0; i < st.length; i++) {
                arr[i] = st[i].arrivalSec();
                dep[i] = st[i].departureSec();
            }
            tripsByKey.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new PatternTrip(trip.id(), trip.headsign(), arr, dep));
            routeByKey.putIfAbsent(key, trip.routeId());
            stopsByKey.putIfAbsent(key, stopIds);
        }

        List<RoutePattern> patterns = new ArrayList<>();
        for (Map.Entry<String, List<PatternTrip>> e : tripsByKey.entrySet()) {
            List<PatternTrip> list = e.getValue();
            list.sort(Comparator.<PatternTrip>comparingInt(t -> t.departures()[0]).thenComparing(PatternTrip::tripId));
            String[] stopIds = stopsByKey.get(e.getKey());
            int[] idx = Arrays.stream(stopIds).mapToInt(stopIndex::get).toArray();
            patterns.add(new RoutePattern(patterns.size(), routeByKey.get(e.getKey()), idx, stopIds,
                    list.toArray(new PatternTrip[0])));
        }
        return patterns;
    }

    private RoutesByStopIndex buildRoutesByStop(List<RoutePattern> patterns, List<Stop> stops) {
        List<List<PatternStopRef>> refs = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            refs.add(new ArrayList<>());
        }
        Map<String, Set<String>> routeIds = new HashMap<>();
        for (RoutePattern p : patterns) {
            for (int pos = 0; pos < p.size(); pos++) {
                refs.get(p.stopIdx()[pos]).add(new PatternStopRef(p.index(), pos));
                routeIds.computeIfAbsent(p.stopIds()[pos], k -> new TreeSet<>()).add(p.routeId());
            }
        }
        return new RoutesByStopIndex(refs, routeIds);
    }

    private List<List<Footpath>> buildFootpaths(List<Stop> stops) {
        SpatialIndexService index = new SpatialIndexService();
        index.rebuild(stops);
        List<List<Footpath>> out = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            Stop s = stops.get(i);
            List<Footpath> list = new ArrayList<>();
            for (SpatialIndexService.StopDistance sd : index.findWithin(s.lat(), s.lon(), footpathRadiusMeters)) {
                if (sd.stopIdx() != i) {
                    list.add(new Footpath(sd.stopIdx(), sd.distanceMeters(), walking.transferSeconds(sd.distanceMeters())));
                }
            }
            out.add(List.copyOf(list));
        }
        return out;
    }

    // ---- helpers -----------------------------------------------------------------------------

    private static List<Map<String, String>> rows(GtfsSource source, String file) throws IOException {
        if (!source.exists(file)) {
            throw new GtfsLoadException("Required GTFS file missing: " + file);
        }
        try (Reader r = source.open(file)) {
            return CsvUtils.read(r);
        }
    }

    private static String req(Map<String, String> row, String column) {
        String v = row.get(column);
        if (v == null || v.isBlank()) {
            throw new GtfsLoadException("Missing required column/value '" + column + "' in row " + row);
        }
        return v;
    }
}
