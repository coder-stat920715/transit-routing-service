package com.transit.routing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class UtilsTest {
    @Test
    void haversineKnownDistance() {
        // 0.01 degree of latitude is about 1112 m
        assertEquals(1112, HaversineDistanceCalculator.distanceMeters(17.0, 78.0, 17.01, 78.0), 2);
        assertEquals(0, HaversineDistanceCalculator.distanceMeters(17.0, 78.0, 17.0, 78.0), 1e-9);
    }

    @Test
    void timeParsingAndFormatting() {
        assertEquals(12 * 3600 + 5 * 60 + 7, TimeUtils.parseGtfsTime("12:05:07"));
        assertEquals(25 * 3600, TimeUtils.parseGtfsTime("25:00:00"));
        assertEquals("14:11", TimeUtils.formatHm(14 * 3600 + 11 * 60 + 30));
        assertEquals("01:00", TimeUtils.formatHm(25 * 3600));
        assertEquals(2, TimeUtils.minutesCeil(61));
    }

    @Test
    void csvHandlesQuotes() {
        assertEquals(List.of("a", "b,c", "d\"e"), CsvUtils.splitLine("a,\"b,c\",\"d\"\"e\""));
    }
}
