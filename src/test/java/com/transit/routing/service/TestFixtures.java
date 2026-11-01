package com.transit.routing.service;

import com.transit.routing.model.raptor.TransitNetwork;
import com.transit.routing.model.raptor.WalkingProfile;

/** Builds the bundled mock GTFS network and engines without a Spring context. */
final class TestFixtures {
    static final WalkingProfile WALKING = WalkingProfile.DEFAULT;

    private TestFixtures() {
    }

    static TransitNetwork network() {
        return new GtfsLoaderService(WALKING, 300).load(GtfsSource.classpath("gtfs"));
    }

    static RaptorRoutingEngineService engine(TransitNetwork network, RoutingStrategy strategy) {
        SpatialIndexService index = new SpatialIndexService();
        index.rebuild(network.stops());
        return new RaptorRoutingEngineService(network, index, strategy, WALKING);
    }
}
