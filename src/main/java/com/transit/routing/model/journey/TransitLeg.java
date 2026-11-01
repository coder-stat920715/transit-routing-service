package com.transit.routing.model.journey;

public record TransitLeg(String routeId, String routeShortName, String headsign, String tripId,
                         String fromStopName, String toStopName, int intermediateStopsCount,
                         int startSec, int endSec) implements Leg {
}
