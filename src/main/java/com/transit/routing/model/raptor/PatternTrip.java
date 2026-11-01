package com.transit.routing.model.raptor;

/** One scheduled run of a route pattern; arrays are indexed by position within the pattern. */
public record PatternTrip(String tripId, String headsign, int[] arrivals, int[] departures) {
}
