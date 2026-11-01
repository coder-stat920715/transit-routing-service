package com.transit.routing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpatialIndexServiceTest {
    private final SpatialIndexService index = new SpatialIndexService();

    SpatialIndexServiceTest() {
        index.rebuild(TestFixtures.network().stops());
    }

    @Test
    void findsOnlyHeritageFreshNearOrigin() {
        List<SpatialIndexService.StopDistance> r = index.findWithin(17.4415, 78.3482, 1000);
        assertEquals(1, r.size());
        assertEquals("Heritage Fresh", r.get(0).stop().name());
        assertEquals(600, r.get(0).distanceMeters(), 10);
    }

    @Test
    void radiusIsRespectedAndSortedNearestFirst() {
        List<SpatialIndexService.StopDistance> r = index.findWithin(17.4260, 78.3700, 300);
        assertEquals(2, r.size());
        assertEquals("Gachibowli X Road", r.get(0).stop().name());
        assertTrue(r.get(0).distanceMeters() <= r.get(1).distanceMeters());
        assertTrue(index.findWithin(17.4260, 78.3700, 50).size() == 1);
    }

    @Test
    void emptyWhenNothingNearby() {
        assertTrue(index.findWithin(0, 0, 1000).isEmpty());
    }
}
