package com.transit.routing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transit.routing.model.gtfs.StopTime;
import com.transit.routing.model.raptor.Footpath;
import com.transit.routing.model.raptor.TransitNetwork;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GtfsLoaderServiceTest {
    private final TransitNetwork network = TestFixtures.network();

    @Test
    void loadsEntitiesAndIndexes() {
        assertEquals(21, network.stopCount());
        assertEquals(4, network.routes().size());
        assertEquals(4, network.patterns().size());
        assertEquals(15, network.tripStopTimes().size());
    }

    @Test
    void routesByStopIndexListsAllRoutesAtInterchange() {
        assertEquals(Set.of("R116", "R195"), network.routesByStop().routeIdsAt("NKG"));
        assertEquals(Set.of("RAJ"), network.routesByStop().routeIdsAt("GAJ"));
    }

    @Test
    void tripStopTimesAreChronological() {
        StopTime[] st = network.tripStopTimes().get("TAJ_1238");
        assertEquals(5, st.length);
        for (int i = 1; i < st.length; i++) {
            assertTrue(st[i].departureSec() > st[i - 1].departureSec());
        }
        assertEquals(12 * 3600 + 38 * 60, st[0].departureSec());
    }

    @Test
    void footpathLinksGachibowliStops() {
        int gx = network.stopIndexById().get("GX");
        int gaj = network.stopIndexById().get("GAJ");
        Footpath fp = network.footpaths().get(gx).stream().filter(f -> f.toStop() == gaj).findFirst().orElseThrow();
        assertEquals(80, fp.distanceMeters(), 5);
        assertEquals(120, fp.seconds());
    }

    @Test
    void patternTripsSortedByFirstDeparture() {
        for (var p : network.patterns()) {
            for (int i = 1; i < p.trips().length; i++) {
                assertTrue(p.trips()[i].departures()[0] >= p.trips()[i - 1].departures()[0]);
            }
        }
    }

    @Test
    void missingRequiredFileFails() {
        GtfsSource empty = new GtfsSource() {
            public boolean exists(String f) {
                return false;
            }

            public Reader open(String f) {
                return new StringReader("");
            }
        };
        assertThrows(GtfsLoadException.class, () -> new GtfsLoaderService(TestFixtures.WALKING, 300).load(empty));
    }

    @Test
    void unknownStopReferenceFails() {
        Map<String, String> files = Map.of(
                "stops.txt", "stop_id,stop_name,stop_lat,stop_lon\nA,A,1,1\n",
                "routes.txt", "route_id,route_short_name\nR,R\n",
                "trips.txt", "route_id,service_id,trip_id,trip_headsign\nR,S,T,H\n",
                "stop_times.txt", "trip_id,arrival_time,departure_time,stop_id,stop_sequence\nT,08:00:00,08:00:00,ZZZ,1\n");
        GtfsSource src = new GtfsSource() {
            public boolean exists(String f) {
                return files.containsKey(f);
            }

            public Reader open(String f) {
                return new StringReader(files.get(f));
            }
        };
        assertThrows(GtfsLoadException.class, () -> new GtfsLoaderService(TestFixtures.WALKING, 300).load(src));
    }
}
