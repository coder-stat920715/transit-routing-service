package com.transit.routing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transit.routing.model.journey.Journey;
import com.transit.routing.model.journey.Leg;
import com.transit.routing.model.journey.RoutingQuery;
import com.transit.routing.model.journey.TransitLeg;
import com.transit.routing.model.journey.WalkLeg;
import com.transit.routing.model.raptor.TransitNetwork;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RaptorRoutingEngineServiceTest {
    private static final double O_LAT = 17.4415, O_LON = 78.3482, D_LAT = 17.2403, D_LON = 78.4294;

    private static TransitNetwork network;
    private static RaptorRoutingEngineService engine;

    @BeforeAll
    static void setUp() {
        network = TestFixtures.network();
        engine = TestFixtures.engine(network, new StaticTimetableRoutingStrategy());
    }

    private static int t(int h, int m) {
        return h * 3600 + m * 60;
    }

    private static RoutingQuery query(int depSec, int maxTransfers, double maxWalk) {
        return new RoutingQuery(O_LAT, O_LON, D_LAT, D_LON, depSec, maxTransfers, maxWalk);
    }

    @Test
    void gowlidoddyToRgia_producesBenchmarkSixLegItinerary() {
        Optional<Journey> result = engine.findBestJourney(query(t(12, 0), 3, 1000));
        assertTrue(result.isPresent());
        Journey j = result.get();
        List<Leg> legs = j.legs();

        assertEquals(6, legs.size());
        assertEquals(2, j.totalTransfers());
        assertEquals(3, j.rounds());
        assertEquals(t(14, 11), j.arrivalSec());
        assertEquals(131 * 60, j.durationSeconds());

        WalkLeg w1 = assertInstanceOf(WalkLeg.class, legs.get(0));
        assertEquals("Zolo Lakeview PG", w1.fromName());
        assertEquals("Heritage Fresh", w1.toName());
        assertEquals(600, w1.distanceMeters(), 10);
        assertEquals(9 * 60, w1.endSec() - w1.startSec());

        TransitLeg b1 = assertInstanceOf(TransitLeg.class, legs.get(1));
        assertEquals("116N/220K", b1.routeShortName());
        assertEquals("Koti Medical College", b1.headsign());
        assertEquals("Heritage Fresh", b1.fromStopName());
        assertEquals("Nanakramguda", b1.toStopName());
        assertEquals(2, b1.intermediateStopsCount());
        assertEquals(6 * 60, b1.endSec() - b1.startSec());
        assertEquals(t(12, 10), b1.startSec());

        TransitLeg b2 = assertInstanceOf(TransitLeg.class, legs.get(2));
        assertEquals("195KG", b2.routeShortName());
        assertEquals("JNTU", b2.headsign());
        assertEquals("Nanakramguda", b2.fromStopName());
        assertEquals("Gachibowli X Road", b2.toStopName());
        assertEquals(4, b2.intermediateStopsCount());
        assertEquals(13 * 60, b2.endSec() - b2.startSec());

        WalkLeg w2 = assertInstanceOf(WalkLeg.class, legs.get(3));
        assertEquals("Gachibowli X Road", w2.fromName());
        assertEquals("Gachibowli", w2.toName());
        assertEquals(80, w2.distanceMeters(), 5);
        assertEquals(2 * 60, w2.endSec() - w2.startSec());

        TransitLeg b3 = assertInstanceOf(TransitLeg.class, legs.get(4));
        assertEquals("AJ", b3.routeShortName());
        assertEquals("Pushpak Airport Express", b3.headsign());
        assertEquals("Gachibowli", b3.fromStopName());
        assertEquals("Rajiv Gandhi International Airport (RGIA)", b3.toStopName());
        assertEquals(3, b3.intermediateStopsCount());
        assertEquals(79 * 60, b3.endSec() - b3.startSec());

        WalkLeg w3 = assertInstanceOf(WalkLeg.class, legs.get(5));
        assertEquals("Rajiv Gandhi International Airport (RGIA)", w3.fromName());
        assertEquals("Domestic Departures Terminal", w3.toName());
        assertEquals(950, w3.distanceMeters(), 10);
        assertEquals(14 * 60, w3.endSec() - w3.startSec());
    }

    @Test
    void legsAreChronologicallyConsistent() {
        Journey j = engine.findBestJourney(query(t(12, 0), 3, 1000)).orElseThrow();
        int clock = j.departureSec();
        for (Leg leg : j.legs()) {
            assertTrue(leg.startSec() >= clock, "leg starts before previous leg ended");
            assertTrue(leg.endSec() >= leg.startSec());
            clock = leg.endSec();
        }
        assertEquals(j.arrivalSec(), clock);
    }

    @Test
    void laterDepartureCatchesLaterTrips() {
        Journey j = engine.findBestJourney(query(t(12, 45), 3, 1000)).orElseThrow();
        assertEquals(t(15, 11), j.arrivalSec());
        assertEquals(6, j.legs().size());
        TransitLeg first = assertInstanceOf(TransitLeg.class, j.legs().get(1));
        assertEquals(t(13, 10), first.startSec());
    }

    @Test
    void requiresTwoTransfers() {
        assertTrue(engine.findBestJourney(query(t(12, 0), 1, 1000)).isEmpty());
        assertTrue(engine.findBestJourney(query(t(12, 0), 2, 1000)).isPresent());
    }

    @Test
    void paretoSetContainsSingleThreeTripJourney() {
        List<Journey> all = engine.findJourneys(query(t(12, 0), 4, 1000));
        assertEquals(1, all.size());
        assertEquals(3, all.get(0).rounds());
    }

    @Test
    void noJourneyWhenServiceHasEnded() {
        assertTrue(engine.findBestJourney(query(t(23, 0), 3, 1000)).isEmpty());
    }

    @Test
    void noJourneyWhenNoStopWithinWalkingRadius() {
        assertTrue(engine.findBestJourney(query(t(12, 0), 3, 500)).isEmpty());
    }

    @Test
    void shortTripUsesDirectWalkingWhenCloser() {
        // From Heritage Fresh coordinates to a point 300 m away: walking only.
        RoutingQuery q = new RoutingQuery(17.4361, 78.3482, 17.4388, 78.3482, t(12, 0), 3, 1000);
        Journey j = engine.findBestJourney(q).orElseThrow();
        assertEquals(1, j.legs().size());
        assertInstanceOf(WalkLeg.class, j.legs().get(0));
        assertEquals(0, j.totalTransfers());
    }

    @Test
    void realtimeStrategyFallsBackToStaticWhenFeedUnavailable() {
        InMemoryRealtimeDelayFeed emptyFeed = new InMemoryRealtimeDelayFeed();
        RoutingStrategy strategy = new RealtimeAwareRoutingStrategy(emptyFeed, new StaticTimetableRoutingStrategy());
        Journey j = TestFixtures.engine(network, strategy).findBestJourney(query(t(12, 0), 3, 1000)).orElseThrow();
        assertEquals(t(14, 11), j.arrivalSec());
    }

    @Test
    void realtimeDelayAtDestinationBusShiftsArrival() {
        InMemoryRealtimeDelayFeed feed = new InMemoryRealtimeDelayFeed();
        feed.setTripDelay("TAJ_1238", 5 * 60);
        RoutingStrategy strategy = new RealtimeAwareRoutingStrategy(feed, new StaticTimetableRoutingStrategy());
        Journey j = TestFixtures.engine(network, strategy).findBestJourney(query(t(12, 0), 3, 1000)).orElseThrow();
        assertEquals(t(14, 16), j.arrivalSec());
    }

    @Test
    void realtimeDelayCausesMissedConnection() {
        InMemoryRealtimeDelayFeed feed = new InMemoryRealtimeDelayFeed();
        feed.setTripDelay("T116_1210", 5 * 60); // reaches Nanakramguda 12:21, misses the 12:20 195KG
        RoutingStrategy strategy = new RealtimeAwareRoutingStrategy(feed, new StaticTimetableRoutingStrategy());
        Journey j = TestFixtures.engine(network, strategy).findBestJourney(query(t(12, 0), 3, 1000)).orElseThrow();
        TransitLeg second = assertInstanceOf(TransitLeg.class, j.legs().get(2));
        assertEquals("195KG", second.routeShortName());
        assertEquals(t(12, 50), second.startSec());
        assertEquals(t(15, 11), j.arrivalSec());
    }
}
