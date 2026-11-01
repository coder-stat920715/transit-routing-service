# transit-routing-service

In-memory GTFS + RAPTOR journey planner. Java 21, Spring Boot 3.3.

## Run
    mvn -N wrapper:wrapper      # once, to generate ./mvnw (or just use mvn)
    ./mvnw spring-boot:run
    ./mvnw test

## Try it
    curl -X POST localhost:8080/api/v1/journey/plan -H 'Content-Type: application/json' -d '{
      "originLatitude":17.4415,"originLongitude":78.3482,
      "destinationLatitude":17.2403,"destinationLongitude":78.4294,
      "departureTime":"2026-10-31T12:00:00","maxTransfers":3,"maxWalkDistanceMeters":1000}'

`POST /api/v1/journey/plan/alternatives` returns the Pareto set (fewer transfers vs. earlier arrival).

## Design notes
- Walk time = straight-line distance x 1.2 (detour) / 1.33 m/s, rounded to whole minutes; stop-to-stop
  transfers have a 120 s minimum. Configurable under `transit.walking.*`.
- Real-time: `transit.realtime.enabled=true` wraps the static timetable with `RealtimeAwareRoutingStrategy`;
  with no feed data it falls back to the static GTFS timetable. The default feed is in-memory
  (`InMemoryRealtimeDelayFeed`); plug a GTFS-RT TripUpdates adapter in by implementing `RealtimeDelayFeed`.
- Service calendars (calendar.txt) are not modelled: all trips run every day.
- Own data: set `transit.gtfs.location` to a directory with stops/routes/trips/stop_times.txt
  (optional places.txt: place_name,place_lat,place_lon labels origin/destination).
