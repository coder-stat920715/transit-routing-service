package com.transit.routing.config;

import com.transit.routing.model.raptor.TransitNetwork;
import com.transit.routing.model.raptor.WalkingProfile;
import com.transit.routing.service.GtfsLoaderService;
import com.transit.routing.service.GtfsSource;
import com.transit.routing.service.InMemoryRealtimeDelayFeed;
import com.transit.routing.service.JourneyPlanningService;
import com.transit.routing.service.RaptorRoutingEngineService;
import com.transit.routing.service.RealtimeAwareRoutingStrategy;
import com.transit.routing.service.RealtimeDelayFeed;
import com.transit.routing.service.RoutingStrategy;
import com.transit.routing.service.SpatialIndexService;
import com.transit.routing.service.StaticTimetableRoutingStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the GTFS loader, in-memory network, spatial index, routing strategy and RAPTOR engine. */
@Configuration
@EnableConfigurationProperties(TransitProperties.class)
public class TransitConfig {

    @Bean
    public WalkingProfile walkingProfile(TransitProperties props) {
        TransitProperties.Walking w = props.walking();
        return new WalkingProfile(w.speedMps(), w.detourFactor(), w.minTransferSeconds());
    }

    @Bean
    public GtfsLoaderService gtfsLoaderService(TransitProperties props, WalkingProfile walkingProfile) {
        return new GtfsLoaderService(walkingProfile, props.walking().footpathRadiusMeters());
    }

    /** GTFS is parsed once at startup (bean creation) and held fully in memory. */
    @Bean
    public TransitNetwork transitNetwork(GtfsLoaderService loader, TransitProperties props) {
        return loader.load(GtfsSource.fromLocation(props.gtfs().location()));
    }

    @Bean
    public SpatialIndexService spatialIndexService(TransitNetwork network) {
        SpatialIndexService index = new SpatialIndexService();
        index.rebuild(network.stops());
        return index;
    }

    @Bean
    public RealtimeDelayFeed realtimeDelayFeed() {
        return new InMemoryRealtimeDelayFeed();
    }

    @Bean
    public RoutingStrategy routingStrategy(TransitProperties props, RealtimeDelayFeed feed) {
        RoutingStrategy staticStrategy = new StaticTimetableRoutingStrategy();
        return props.realtime().enabled() ? new RealtimeAwareRoutingStrategy(feed, staticStrategy) : staticStrategy;
    }

    @Bean
    public RaptorRoutingEngineService raptorRoutingEngineService(TransitNetwork network, SpatialIndexService index,
                                                                 RoutingStrategy strategy, WalkingProfile walking) {
        return new RaptorRoutingEngineService(network, index, strategy, walking);
    }

    @Bean
    public JourneyPlanningService journeyPlanningService(RaptorRoutingEngineService engine) {
        return new JourneyPlanningService(engine);
    }
}
