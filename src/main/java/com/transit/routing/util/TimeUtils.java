package com.transit.routing.util;

import java.time.LocalDateTime;

/** Time helpers. All internal times are seconds since midnight of the service day (may exceed 86400). */
public final class TimeUtils {
    private TimeUtils() {
    }

    /** Parses GTFS "H:MM:SS" / "HH:MM:SS" (hours may be >= 24). */
    public static int parseGtfsTime(String value) {
        String[] p = value.trim().split(":");
        if (p.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time: '" + value + "'");
        }
        return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60 + Integer.parseInt(p[2]);
    }

    public static int secondsOfDay(LocalDateTime dt) {
        return dt.toLocalTime().toSecondOfDay();
    }

    /** "HH:mm" (wraps after 24h). */
    public static String formatHm(int seconds) {
        int s = Math.floorMod(seconds, 86_400);
        return String.format("%02d:%02d", s / 3600, (s % 3600) / 60);
    }

    /** Duration in whole minutes, rounded up. */
    public static int minutesCeil(int seconds) {
        return (seconds + 59) / 60;
    }

    /** Midnight of the given day plus an offset in seconds (offset may exceed 24h). */
    public static LocalDateTime toDateTime(LocalDateTime anyTimeOnServiceDay, int secondsOfServiceDay) {
        return anyTimeOnServiceDay.toLocalDate().atStartOfDay().plusSeconds(secondsOfServiceDay);
    }
}
