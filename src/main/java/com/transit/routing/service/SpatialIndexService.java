package com.transit.routing.service;

import com.transit.routing.model.gtfs.Stop;
import com.transit.routing.util.HaversineDistanceCalculator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uniform-grid spatial index (0.01 degree cells, ~1.1 km) with exact Haversine filtering.
 * Supports radius queries around any coordinate.
 */
public class SpatialIndexService {
    public record StopDistance(Stop stop, int stopIdx, double distanceMeters) {
    }

    private static final double CELL_DEG = 0.01;
    private static final double METERS_PER_DEG_LAT = 111_195.0;

    private final Map<Long, List<Integer>> grid = new HashMap<>();
    private List<Stop> stops = List.of();

    public void rebuild(Collection<Stop> stopCollection) {
        grid.clear();
        stops = List.copyOf(stopCollection);
        for (int i = 0; i < stops.size(); i++) {
            Stop s = stops.get(i);
            grid.computeIfAbsent(key(cell(s.lat()), cell(s.lon())), k -> new ArrayList<>()).add(i);
        }
    }

    /** All stops within radiusMeters (inclusive), nearest first. */
    public List<StopDistance> findWithin(double lat, double lon, double radiusMeters) {
        double cosLat = Math.max(0.01, Math.cos(Math.toRadians(lat)));
        int latCells = (int) Math.ceil(radiusMeters / (METERS_PER_DEG_LAT * CELL_DEG)) + 1;
        int lonCells = (int) Math.ceil(radiusMeters / (METERS_PER_DEG_LAT * cosLat * CELL_DEG)) + 1;
        long cy = cell(lat);
        long cx = cell(lon);
        List<StopDistance> out = new ArrayList<>();
        for (long y = cy - latCells; y <= cy + latCells; y++) {
            for (long x = cx - lonCells; x <= cx + lonCells; x++) {
                List<Integer> bucket = grid.get(key(y, x));
                if (bucket == null) {
                    continue;
                }
                for (int idx : bucket) {
                    Stop s = stops.get(idx);
                    double d = HaversineDistanceCalculator.distanceMeters(lat, lon, s.lat(), s.lon());
                    if (d <= radiusMeters) {
                        out.add(new StopDistance(s, idx, d));
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble(StopDistance::distanceMeters));
        return out;
    }

    private static long cell(double deg) {
        return (long) Math.floor(deg / CELL_DEG);
    }

    private static long key(long y, long x) {
        return (y << 32) ^ (x & 0xffffffffL);
    }
}
