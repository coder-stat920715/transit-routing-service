package com.transit.routing.service;

import com.transit.routing.model.journey.Journey;
import com.transit.routing.model.journey.JourneyRequest;
import com.transit.routing.model.journey.JourneyResponse;
import com.transit.routing.model.journey.Leg;
import com.transit.routing.model.journey.LegResponse;
import com.transit.routing.model.journey.RoutingQuery;
import com.transit.routing.model.journey.TransitLeg;
import com.transit.routing.model.journey.WalkLeg;
import com.transit.routing.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/** Maps API requests to engine queries and engine journeys to API responses. */
public class JourneyPlanningService {
    static final int DEFAULT_MAX_TRANSFERS = 3;
    static final int DEFAULT_MAX_WALK_METERS = 1000;

    private final RaptorRoutingEngineService engine;

    public JourneyPlanningService(RaptorRoutingEngineService engine) {
        this.engine = engine;
    }

    public JourneyResponse plan(JourneyRequest request) {
        List<Journey> journeys = engine.findJourneys(toQuery(request));
        if (journeys.isEmpty()) {
            throw new NoJourneyFoundException("No journey found for the requested time, transfers and walking limits");
        }
        return toResponse(journeys.get(journeys.size() - 1), request);
    }

    /** Pareto set (fewer trips first, each strictly faster than the previous). */
    public List<JourneyResponse> planAlternatives(JourneyRequest request) {
        List<Journey> journeys = engine.findJourneys(toQuery(request));
        if (journeys.isEmpty()) {
            throw new NoJourneyFoundException("No journey found for the requested time, transfers and walking limits");
        }
        List<JourneyResponse> out = new ArrayList<>();
        for (Journey j : journeys) {
            out.add(toResponse(j, request));
        }
        return out;
    }

    private RoutingQuery toQuery(JourneyRequest r) {
        return new RoutingQuery(r.originLatitude(), r.originLongitude(),
                r.destinationLatitude(), r.destinationLongitude(),
                TimeUtils.secondsOfDay(r.departureTime()),
                r.maxTransfers() != null ? r.maxTransfers() : DEFAULT_MAX_TRANSFERS,
                r.maxWalkDistanceMeters() != null ? r.maxWalkDistanceMeters() : DEFAULT_MAX_WALK_METERS);
    }

    JourneyResponse toResponse(Journey j, JourneyRequest request) {
        List<LegResponse> legs = new ArrayList<>();
        for (Leg leg : j.legs()) {
            legs.add(switch (leg) {
                case WalkLeg w -> new LegResponse("WALK", w.fromName(), w.toName(),
                        (int) Math.round(w.distanceMeters()), null, null, null, null, null,
                        TimeUtils.minutesCeil(w.endSec() - w.startSec()),
                        TimeUtils.formatHm(w.startSec()), TimeUtils.formatHm(w.endSec()));
                case TransitLeg t -> new LegResponse("TRANSIT", null, null, null,
                        t.routeShortName(), t.headsign(), t.fromStopName(), t.toStopName(),
                        t.intermediateStopsCount(),
                        TimeUtils.minutesCeil(t.endSec() - t.startSec()),
                        TimeUtils.formatHm(t.startSec()), TimeUtils.formatHm(t.endSec()));
            });
        }
        return new JourneyResponse(TimeUtils.minutesCeil(j.durationSeconds()),
                TimeUtils.toDateTime(request.departureTime(), j.departureSec()),
                TimeUtils.toDateTime(request.departureTime(), j.arrivalSec()),
                j.totalTransfers(), legs);
    }
}
