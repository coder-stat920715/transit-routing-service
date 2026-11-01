package com.transit.routing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JourneyControllerIntegrationTest {
    private static final String BENCHMARK = """
            {"originLatitude":17.4415,"originLongitude":78.3482,
             "destinationLatitude":17.2403,"destinationLongitude":78.4294,
             "departureTime":"2026-10-31T12:00:00","maxTransfers":3,"maxWalkDistanceMeters":1000}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void plansBenchmarkJourney() throws Exception {
        mvc.perform(post("/api/v1/journey/plan").contentType(MediaType.APPLICATION_JSON).content(BENCHMARK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(131))
                .andExpect(jsonPath("$.totalTransfers").value(2))
                .andExpect(jsonPath("$.departureTime").value("2026-10-31T12:00:00"))
                .andExpect(jsonPath("$.arrivalTime").value("2026-10-31T14:11:00"))
                .andExpect(jsonPath("$.legs.length()").value(6))
                .andExpect(jsonPath("$.legs[0].legType").value("WALK"))
                .andExpect(jsonPath("$.legs[0].fromName").value("Zolo Lakeview PG"))
                .andExpect(jsonPath("$.legs[0].toName").value("Heritage Fresh"))
                .andExpect(jsonPath("$.legs[0].durationMinutes").value(9))
                .andExpect(jsonPath("$.legs[1].routeShortName").value("116N/220K"))
                .andExpect(jsonPath("$.legs[1].intermediateStopsCount").value(2))
                .andExpect(jsonPath("$.legs[1].durationMinutes").value(6))
                .andExpect(jsonPath("$.legs[2].routeShortName").value("195KG"))
                .andExpect(jsonPath("$.legs[2].durationMinutes").value(13))
                .andExpect(jsonPath("$.legs[3].toName").value("Gachibowli"))
                .andExpect(jsonPath("$.legs[4].routeShortName").value("AJ"))
                .andExpect(jsonPath("$.legs[4].durationMinutes").value(79))
                .andExpect(jsonPath("$.legs[5].toName").value("Domestic Departures Terminal"))
                .andExpect(jsonPath("$.legs[5].durationMinutes").value(14));
    }

    @Test
    void returnsNotFoundWhenNoService() throws Exception {
        mvc.perform(post("/api/v1/journey/plan").contentType(MediaType.APPLICATION_JSON)
                        .content(BENCHMARK.replace("12:00:00", "23:00:00")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mvc.perform(post("/api/v1/journey/plan").contentType(MediaType.APPLICATION_JSON)
                        .content(BENCHMARK.replace("\"maxTransfers\":3", "\"maxTransfers\":99")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsParetoAlternatives() throws Exception {
        mvc.perform(post("/api/v1/journey/plan/alternatives").contentType(MediaType.APPLICATION_JSON).content(BENCHMARK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
