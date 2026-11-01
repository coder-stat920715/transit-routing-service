package com.transit.routing.service;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/** Simple thread-safe delay store (default feed; empty = "no GTFS-RT signal"). */
public class InMemoryRealtimeDelayFeed implements RealtimeDelayFeed {
    private final Map<String, Integer> delayByTrip = new ConcurrentHashMap<>();

    public void setTripDelay(String tripId, int delaySeconds) {
        delayByTrip.put(tripId, delaySeconds);
    }

    public void clear() {
        delayByTrip.clear();
    }

    @Override
    public boolean isAvailable() {
        return !delayByTrip.isEmpty();
    }

    @Override
    public OptionalInt delaySeconds(String tripId, String stopId) {
        Integer d = delayByTrip.get(tripId);
        return d == null ? OptionalInt.empty() : OptionalInt.of(d);
    }
}
