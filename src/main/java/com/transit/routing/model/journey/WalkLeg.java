package com.transit.routing.model.journey;

public record WalkLeg(String fromName, String toName, double distanceMeters, int startSec, int endSec) implements Leg {
}
