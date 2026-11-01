# Testing Guide – transit-routing-service

## 1. Prerequisites
- JDK 21, Maven 3.9+ (or `./mvnw` after `mvn -N wrapper:wrapper`)
- Postman (desktop or web agent) – optionally Node.js for `newman`

## 2. Start the service
```
./mvnw spring-boot:run
```
Expected log line (about 5 seconds after start):
```
GTFS loaded: 21 stops, 4 routes, 4 patterns, 15 trips; strategy=STATIC_TIMETABLE
```
If you see `GtfsLoadException`, the GTFS files are missing or malformed (check `transit.gtfs.location`).
The app listens on `http://localhost:8080`. If port 8080 is taken, run with `--server.port=9090` and change `baseUrl` below.

## 3. Import into Postman
1. Postman -> **Import** -> select `postman/transit-routing-service.postman_collection.json`.
2. Open the collection -> **Variables** tab. `baseUrl` defaults to `http://localhost:8080`. Edit **Current value** if needed, then **Save**.
3. Postman Web users: use the Desktop Agent, otherwise `localhost` is unreachable.

## 4. Run requests one by one
Open a request, press **Send**, then check the **Test Results** tab (all tests green = pass).

### Folder 1 – Happy path
| # | Request | Expected |
|---|---------|----------|
| 1.1 | Benchmark 12:00 | 200. Duration 131 min, 2 transfers, arrival `2026-10-31T14:11:00`, 6 legs (see table below) |
| 1.2 | Departure 12:45 | 200. First bus starts 13:10, arrival `15:11`, duration 146 min |
| 1.3 | Optional fields omitted | 200. Same 6 legs (defaults: 3 transfers, 1000 m) |
| 1.4 | Direct walk | 200. One WALK leg, 0 transfers |
| 1.5 | Alternatives | 200. JSON array with one journey (6 legs) |

Benchmark legs (request 1.1):

| Leg | Type | Detail | Time |
|-----|------|--------|------|
| 1 | WALK | Zolo Lakeview PG -> Heritage Fresh, ~600 m, 9 min | 12:00-12:09 |
| 2 | TRANSIT | 116N/220K (Koti Medical College), Heritage Fresh -> Nanakramguda, 2 intermediate stops, 6 min | 12:10-12:16 |
| 3 | TRANSIT | 195KG (JNTU), Nanakramguda -> Gachibowli X Road, 4 intermediate stops, 13 min | 12:20-12:33 |
| 4 | WALK | Gachibowli X Road -> Gachibowli, ~80 m, 2 min | 12:33-12:35 |
| 5 | TRANSIT | AJ (Pushpak Airport Express), Gachibowli -> RGIA, 3 intermediate stops, 79 min | 12:38-13:57 |
| 6 | WALK | RGIA -> Domestic Departures Terminal, ~950 m, 14 min | 13:57-14:11 |

Waiting time (e.g. 12:16-12:20 at Nanakramguda) is not a leg; it is part of the total duration.

### Folder 2 – Routing constraints (expect 404)
| # | Request | Why |
|---|---------|-----|
| 2.1 | maxTransfers = 1 | Journey needs 3 trips = 2 transfers |
| 2.2 | maxTransfers = 2 | Boundary: succeeds (200) |
| 2.3 | maxWalkDistance = 500 | Nearest stop is ~600 m away |
| 2.4 | Departure 23:00 | No service after the last trips |
| 2.5 | Origin (0,0) | No stop within walking radius |

404 body (RFC 7807 problem detail):
```
{"type":"about:blank","title":"Not Found","status":404,"detail":"No journey found for the requested time, transfers and walking limits","instance":"/api/v1/journey/plan"}
```

### Folder 3 – Validation (expect 400)
maxTransfers 99, maxWalkDistance 10, latitude 200, missing departureTime, malformed JSON, bad date format.
Example body: `{"status":400,"detail":"maxTransfers: must be less than or equal to 8", ...}`

## 5. Run everything at once (Collection Runner)
1. Right-click the collection -> **Run collection**.
2. Keep the default order, iterations = 1, delay 0 ms -> **Run**.
3. Expected result: **16 requests, all tests passed, 0 failed**.

## 6. Command line (CI) with newman
```
npm install -g newman
newman run postman/transit-routing-service.postman_collection.json --env-var baseUrl=http://localhost:8080
```
Exit code 0 means everything passed.

## 7. Equivalent curl
```
curl -s -X POST http://localhost:8080/api/v1/journey/plan \
  -H 'Content-Type: application/json' \
  -d '{"originLatitude":17.4415,"originLongitude":78.3482,"destinationLatitude":17.2403,
       "destinationLongitude":78.4294,"departureTime":"2026-10-31T12:00:00",
       "maxTransfers":3,"maxWalkDistanceMeters":1000}'
```

## 8. Automated Java tests
```
./mvnw test
```
Covers the engine (benchmark itinerary, later departures, missed connections via real-time delay, walking limits, transfer limits), GTFS loader, spatial index, utilities, and a MockMvc integration test of the REST endpoint.

## 9. Real-time fallback check
- With the default config (`transit.realtime.enabled=false`) the strategy is `STATIC_TIMETABLE` (see startup log).
- With `--transit.realtime.enabled=true` and no delay data, results are identical to the static run (fallback). Delay injection is covered by the JUnit tests (`realtimeDelay...`), since the default feed is in-memory with no REST endpoint.

## 10. Troubleshooting
| Symptom | Cause / fix |
|---------|-------------|
| `Could not send request` / ECONNREFUSED | App not running or wrong `baseUrl`/port |
| 404 on the benchmark request | Departure time after 13:xx on a different time-of-day, or `maxTransfers` < 2, or `maxWalkDistanceMeters` < 950 |
| 400 `Malformed JSON request body` | Missing Content-Type or invalid JSON; date must be `yyyy-MM-ddTHH:mm:ss` |
| 405 Method Not Allowed | Endpoint accepts POST only |
| Postman Web cannot reach localhost | Install/enable the Postman Desktop Agent |
