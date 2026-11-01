package com.transit.routing.model.raptor;

import com.transit.routing.model.gtfs.StopTime;

import java.util.Map;

/** TripId -> chronologically ordered stop times. */
public final class TripStopTimesMap {
    private final Map<String, StopTime[]> byTrip;

    public TripStopTimesMap(Map<String, StopTime[]> byTrip) {
        this.byTrip = byTrip;
    }

    public StopTime[] get(String tripId) {
        return byTrip.get(tripId);
    }

    public int size() {
        return byTrip.size();
    }

    public int[] departures(String tripId) {
        StopTime[] st = byTrip.get(tripId);
        int[] out = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            out[i] = st[i].departureSec();
        }
        return out;
    }
}
