package com.transit.routing.model.gtfs;

/** Named point of interest used to label origin/destination coordinates. */
public record Place(String name, double lat, double lon) {
}
