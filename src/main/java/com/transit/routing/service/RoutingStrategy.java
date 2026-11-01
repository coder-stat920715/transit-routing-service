package com.transit.routing.service;

import com.transit.routing.model.raptor.PatternTrip;
import com.transit.routing.model.raptor.RoutePattern;

/** ETA calculation strategy: supplies the arrival/departure second for a trip at a pattern position. */
public interface RoutingStrategy {
    int arrivalSeconds(RoutePattern pattern, PatternTrip trip, int position);

    int departureSeconds(RoutePattern pattern, PatternTrip trip, int position);

    String name();
}
