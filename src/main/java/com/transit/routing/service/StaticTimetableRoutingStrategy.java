package com.transit.routing.service;

import com.transit.routing.model.raptor.PatternTrip;
import com.transit.routing.model.raptor.RoutePattern;

/** Uses the static GTFS timetable only. */
public class StaticTimetableRoutingStrategy implements RoutingStrategy {
    @Override
    public int arrivalSeconds(RoutePattern pattern, PatternTrip trip, int position) {
        return trip.arrivals()[position];
    }

    @Override
    public int departureSeconds(RoutePattern pattern, PatternTrip trip, int position) {
        return trip.departures()[position];
    }

    @Override
    public String name() {
        return "STATIC_TIMETABLE";
    }
}
