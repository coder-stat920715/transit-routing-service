package com.transit.routing.util;

/** Great-circle distance on a spherical Earth. */
public final class HaversineDistanceCalculator {
    public static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private HaversineDistanceCalculator() {
    }

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = p2 - p1;
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
