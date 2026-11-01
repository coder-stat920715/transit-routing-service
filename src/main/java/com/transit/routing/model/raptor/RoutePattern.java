package com.transit.routing.model.raptor;

/**
 * RAPTOR "route": all trips of one GTFS route sharing the exact same ordered stop sequence.
 * Trips are sorted by departure time at the first stop.
 */
public record RoutePattern(int index, String routeId, int[] stopIdx, String[] stopIds, PatternTrip[] trips) {
    public int size() {
        return stopIdx.length;
    }
}
