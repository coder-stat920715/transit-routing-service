package com.transit.routing.model.journey;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Flat API leg: WALK legs fill from/to names + distance, TRANSIT legs fill route fields. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LegResponse(String legType,
                          String fromName, String toName, Integer distanceMeters,
                          String routeShortName, String headsign,
                          String fromStopName, String toStopName, Integer intermediateStopsCount,
                          int durationMinutes, String startTime, String endTime) {
}
