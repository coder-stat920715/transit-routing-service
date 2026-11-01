package com.transit.routing.model.journey;

import java.time.LocalDateTime;
import java.util.List;

public record JourneyResponse(int totalDurationMinutes, LocalDateTime departureTime, LocalDateTime arrivalTime,
                              int totalTransfers, List<LegResponse> legs) {
}
