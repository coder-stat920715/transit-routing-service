package com.transit.routing.model.journey;

import java.util.List;

/** A complete itinerary. {@code rounds} = number of RAPTOR rounds (= transit trips) used. */
public record Journey(List<Leg> legs, int departureSec, int arrivalSec, int rounds) {
    public int totalTransfers() {
        long trips = legs.stream().filter(l -> l instanceof TransitLeg).count();
        return (int) Math.max(0, trips - 1);
    }

    public int durationSeconds() {
        return arrivalSec - departureSec;
    }
}
