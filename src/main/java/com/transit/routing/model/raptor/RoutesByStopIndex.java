package com.transit.routing.model.raptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** StopId -> routes passing through it (route ids, plus RAPTOR pattern positions for scanning). */
public final class RoutesByStopIndex {
    private final List<List<PatternStopRef>> patternRefsByStopIdx;
    private final Map<String, Set<String>> routeIdsByStopId;

    public RoutesByStopIndex(List<List<PatternStopRef>> patternRefsByStopIdx,
                             Map<String, Set<String>> routeIdsByStopId) {
        this.patternRefsByStopIdx = patternRefsByStopIdx;
        this.routeIdsByStopId = routeIdsByStopId;
    }

    public List<PatternStopRef> patternsAt(int stopIdx) {
        return patternRefsByStopIdx.get(stopIdx);
    }

    public Set<String> routeIdsAt(String stopId) {
        return routeIdsByStopId.getOrDefault(stopId, Set.of());
    }
}
