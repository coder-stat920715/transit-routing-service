package com.transit.routing.model.journey;

public record RoutingQuery(double originLat, double originLon, double destLat, double destLon,
                           int departureSec, int maxTransfers, double maxWalkMeters) {
}
