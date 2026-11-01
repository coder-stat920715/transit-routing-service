package com.transit.routing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "transit")
public record TransitProperties(@DefaultValue Gtfs gtfs, @DefaultValue Walking walking, @DefaultValue Realtime realtime) {

    public record Gtfs(@DefaultValue("classpath:gtfs") String location) {
    }

    public record Walking(@DefaultValue("1.33") double speedMps,
                          @DefaultValue("1.2") double detourFactor,
                          @DefaultValue("300") double footpathRadiusMeters,
                          @DefaultValue("120") int minTransferSeconds) {
    }

    public record Realtime(@DefaultValue("false") boolean enabled) {
    }
}
