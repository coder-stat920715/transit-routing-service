package com.transit.routing.service;

import java.util.OptionalInt;

/** Source of GTFS-RT style per-trip delays. Implementations may wrap a protobuf TripUpdate feed. */
public interface RealtimeDelayFeed {
    /** False when the feed is disabled, unreachable or stale. */
    boolean isAvailable();

    OptionalInt delaySeconds(String tripId, String stopId);
}
