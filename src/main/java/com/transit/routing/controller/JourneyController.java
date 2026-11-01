package com.transit.routing.controller;

import com.transit.routing.model.journey.JourneyRequest;
import com.transit.routing.model.journey.JourneyResponse;
import com.transit.routing.service.JourneyPlanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/journey")
@RequiredArgsConstructor
public class JourneyController {
    private final JourneyPlanningService planningService;

    /** Earliest-arrival itinerary. */
    @PostMapping("/plan")
    public JourneyResponse plan(@Valid @RequestBody JourneyRequest request) {
        log.debug("Journey request {}", request);
        return planningService.plan(request);
    }

    /** Pareto set of itineraries (fewer transfers vs. earlier arrival). */
    @PostMapping("/plan/alternatives")
    public List<JourneyResponse> alternatives(@Valid @RequestBody JourneyRequest request) {
        return planningService.planAlternatives(request);
    }
}
