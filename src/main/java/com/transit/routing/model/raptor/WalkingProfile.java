package com.transit.routing.model.raptor;

/**
 * Walking model. Walk time = straight-line distance x detourFactor / speed, rounded to whole minutes
 * (minimum 1). Stop-to-stop transfers additionally respect a minimum transfer time.
 */
public record WalkingProfile(double speedMps, double detourFactor, int minTransferSeconds) {
    public static final WalkingProfile DEFAULT = new WalkingProfile(1.33, 1.2, 120);

    public int walkSeconds(double distanceMeters) {
        if (distanceMeters <= 0.5) {
            return 0;
        }
        double raw = distanceMeters * detourFactor / speedMps;
        return Math.max(60, (int) Math.round(raw / 60.0) * 60);
    }

    public int transferSeconds(double distanceMeters) {
        return Math.max(minTransferSeconds, walkSeconds(distanceMeters));
    }
}
