package com.transit.routing.model.journey;

/** Journey leg (domain model). Times are seconds since midnight of the service day. */
public sealed interface Leg permits WalkLeg, TransitLeg {
    int startSec();

    int endSec();
}
