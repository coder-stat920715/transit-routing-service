package com.transit.routing.service;

import com.transit.routing.model.raptor.PatternTrip;
import com.transit.routing.model.raptor.RoutePattern;

import java.util.OptionalInt;

/**
 * Applies real-time delays when the feed is available and has data for the trip;
 * otherwise falls back to the static timetable.
 */
public class RealtimeAwareRoutingStrategy implements RoutingStrategy {
    private final RealtimeDelayFeed feed;
    private final RoutingStrategy fallback;

    public RealtimeAwareRoutingStrategy(RealtimeDelayFeed feed, RoutingStrategy fallback) {
        this.feed = feed;
        this.fallback = fallback;
    }

    @Override
    public int arrivalSeconds(RoutePattern pattern, PatternTrip trip, int position) {
        return fallback.arrivalSeconds(pattern, trip, position) + delay(pattern, trip, position);
    }

    @Override
    public int departureSeconds(RoutePattern pattern, PatternTrip trip, int position) {
        return fallback.departureSeconds(pattern, trip, position) + delay(pattern, trip, position);
    }

    private int delay(RoutePattern pattern, PatternTrip trip, int position) {
        if (feed == null || !feed.isAvailable()) {
            return 0;
        }
        OptionalInt d = feed.delaySeconds(trip.tripId(), pattern.stopIds()[position]);
        return d.orElse(0);
    }

    @Override
    public String name() {
        return feed != null && feed.isAvailable() ? "REALTIME_WITH_STATIC_FALLBACK" : "STATIC_TIMETABLE(fallback)";
    }
}
