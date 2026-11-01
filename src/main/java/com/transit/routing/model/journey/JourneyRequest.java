package com.transit.routing.model.journey;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record JourneyRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double originLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double originLongitude,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double destinationLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double destinationLongitude,
        @NotNull LocalDateTime departureTime,
        @Min(0) @Max(8) Integer maxTransfers,
        @Min(50) @Max(2000) Integer maxWalkDistanceMeters) {
}
