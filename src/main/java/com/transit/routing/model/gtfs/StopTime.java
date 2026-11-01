package com.transit.routing.model.gtfs;

public record StopTime(String tripId, int arrivalSec, int departureSec, String stopId, int stopSequence) {
}
