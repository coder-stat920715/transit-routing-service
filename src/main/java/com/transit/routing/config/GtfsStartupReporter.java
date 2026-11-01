package com.transit.routing.config;

import com.transit.routing.model.raptor.TransitNetwork;
import com.transit.routing.service.RoutingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Logs a summary of the GTFS data loaded into memory at startup. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GtfsStartupReporter implements ApplicationRunner {
    private final TransitNetwork network;
    private final RoutingStrategy strategy;

    @Override
    public void run(ApplicationArguments args) {
        log.info("GTFS loaded: {} stops, {} routes, {} patterns, {} trips; strategy={}",
                network.stopCount(), network.routes().size(), network.patterns().size(),
                network.tripStopTimes().size(), strategy.name());
    }
}
