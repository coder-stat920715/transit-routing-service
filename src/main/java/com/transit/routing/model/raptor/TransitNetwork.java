package com.transit.routing.model.raptor;

import com.transit.routing.model.gtfs.GtfsRoute;
import com.transit.routing.model.gtfs.Place;
import com.transit.routing.model.gtfs.Stop;
import com.transit.routing.util.HaversineDistanceCalculator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable, fully indexed in-memory transit network used by RAPTOR. */
public record TransitNetwork(List<Stop> stops,
                             Map<String, Integer> stopIndexById,
                             Map<String, GtfsRoute> routes,
                             List<RoutePattern> patterns,
                             RoutesByStopIndex routesByStop,
                             TripStopTimesMap tripStopTimes,
                             List<List<Footpath>> footpaths,
                             List<Place> places) {

    public Stop stop(int idx) {
        return stops.get(idx);
    }

    public int stopCount() {
        return stops.size();
    }

    public Optional<Place> nearestPlace(double lat, double lon, double withinMeters) {
        Place best = null;
        double bestD = withinMeters;
        for (Place p : places) {
            double d = HaversineDistanceCalculator.distanceMeters(lat, lon, p.lat(), p.lon());
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return Optional.ofNullable(best);
    }
}
