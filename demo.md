Demo plan — Swifteats delivery platform (9–10 minutes)

Goal
- Deliver a focused 9–10 minute demo that explains design and decisions, and then walks through a live end‑to‑end run of the system with emphasis on the live demo portion.

Timing (total 9–10 minutes)
- 0:00–1:00 — Quick intro + objectives (60s)
- 1:00–3:00 — Design & architecture (120s)
- 3:00–4:00 — Project journey & interaction with coding assistant (60s)
- 4:00–5:00 — Key decisions and trade‑offs (60s)
- 5:00–9:00 — Live demo: full working flow with highlights (4 min) + 1 min buffer for closing (60s)

1) Intro (0:00–1:00)
- One‑line summary: a modular microservice demo showing catalog, ordering (transactional outbox), payments (mock), tracking (Redis + SSE) and a simulator that exercises real flows.
- Objective of demo: validate business logic (caching, outbox, eventing, realtime tracking) working together.

2) Design & Architecture (1:00–3:00)
- High level components:
  - gateway (API façade)
  - catalog-service (restaurants + menu, Redis caching)
  - order-service (orders + transactional outbox + OutboxPoller)
  - payment-mock-service (consumes payment requests and emits results)
  - tracking-ingest-service (ingests driver GPS and writes latest location to Redis + publishes events)
  - tracking-query-service (SSE endpoint to stream driver location to clients)
  - simulator (drives driver location updates to exercise tracking pipeline)
  - infra: Postgres (persistence), Redis (cache + realtime key store), RabbitMQ (message broker)

- Key interaction patterns:
  - Synchronous HTTP for commands and queries (catalog, order, tracking ingest).
  - Redis as the cache for read‑heavy menu queries (Spring Cache + RedisCacheManager). Writes evict cache keys.
  - Transactional Outbox: Order creation persists the order row and an OutboxEvent in a single DB transaction. A poller publishes outbox events to RabbitMQ, keeping transactional guarantees without distributed transactions.
  - RabbitMQ for asynchronous decoupled communication: order events → payments, payments → results.
  - SSE (SseEmitter) in tracking-query-service for realtime streaming to clients; tracking-ingest writes latest location to Redis key driver.location.{driverId} so SSE readers can poll/receive deltas efficiently.

- Why this architecture
  - Reliable delivery: outbox avoids lost messages when the publisher and DB share a transaction boundary.
  - Scalability: caches for read throughput; RabbitMQ for async scaling and replay; Redis for very low‑latency location reads.
  - Modularity: services can be developed, tested, and deployed independently.

3) Journey from brainstorming to implementation (3:00–4:00)
- Brainstorming: defined Phase‑2 goals (catalog caching, outbox poller, multi‑driver simulator for tracking). Chose technologies that match constraints: Java 21 + Spring Boot, Postgres, Redis, RabbitMQ.
- Implementation steps:
  - Implemented Redis cache config and annotated CatalogService methods with @Cacheable/@CacheEvict.
  - Implemented transactional outbox (OutboxEvent entity and OutboxPoller) in order-service and tests that validate publish semantics.
  - Implemented tracking ingest + query (Redis key writes, SSE streaming) and a simulator to drive updates.
- Interaction with coding assistant: used the assistant to iterate configuration fixes, test support with Testcontainers, and diagnose startup issues (DB host resolution and Flyway migrations). Conversation influenced quick run strategies (override hostnames when running on host, or disable Flyway for a fast demo).

4) Key decisions and trade‑offs (4:00–5:00)
- Outbox vs XA or distributed transactions: chosen outbox for simplicity, portability, and easier testing—trades off the need for a poller and eventual delivery guarantees instead of synchronous commit to broker.
- Redis caching for menus: low latency cache with explicit eviction on updates. Trade‑off: cache invalidation complexity handled via @CacheEvict rules.
- Running services on host vs inside containers: host runs require env overrides (POSTGRES_HOST=localhost etc.). Running inside Docker makes service hostnames resolve automatically—trade‑off is increased orchestration complexity vs convenience for local debugging.
- Flyway on startup vs pre‑migrate: running migrations on startup is convenient but can complicate demo when host resolution is misconfigured; for demo we optionally disable Flyway and seed data.

5) Live demo plan (5:00–9:00) — focus of the presentation (4 minutes)
- Preparation (do these beforehand or run quickly on camera):
  - Start infra: docker compose up -d (Postgres 5432, Redis 6379, RabbitMQ 5672/15672)
  - Decide mode: (A) run services on host with env overrides + SPRING_FLYWAY_ENABLED=false (fast), or (B) run services inside containers (default hostnames work).

- Exact commands to run (presenter copy/paste)
  - Start infra (already running in demo environment):
    docker compose up -d

  - Start catalog-service (host mode, fast demo - disables Flyway)
    POSTGRES_HOST=localhost REDIS_HOST=localhost RABBITMQ_HOST=localhost \
    POSTGRES_DB=swifteatsdb POSTGRES_USER=swifteats POSTGRES_PASSWORD=swifteats \
    SPRING_FLYWAY_ENABLED=false SERVER_PORT=8081 ./mvnw -pl :catalog-service spring-boot:run > .logs/catalog.log 2>&1 & echo $! > .pids/catalog.pid

  - Start order-service (host mode)
    POSTGRES_HOST=localhost REDIS_HOST=localhost RABBITMQ_HOST=localhost \
    POSTGRES_DB=swifteatsdb POSTGRES_USER=swifteats POSTGRES_PASSWORD=swifteats \
    SPRING_FLYWAY_ENABLED=false SERVER_PORT=8082 ./mvnw -pl :order-service spring-boot:run > .logs/order.log 2>&1 & echo $! > .pids/order.pid

  - Start payment-mock-service, tracking-ingest-service, tracking-query-service, simulator with ports 8083..8086 same pattern.

- Demo flow (what to show, in order)
  1) Verify infra and services are up
     - Show docker compose ps and service /ping endpoints (catalog /ping at :8081)
     - curl http://localhost:8081/ping → expected: {"status":"ok","service":"catalog"}

  2) Catalog create + cache demonstration (show cache behavior)
     - POST /restaurants to create an example restaurant (show request body and server response). Example:
       curl -X POST http://localhost:8081/restaurants -H 'Content-Type: application/json' -d '{"name":"Demo Pizza","menu":[{"id":"m1","name":"Margherita","price":9.99}]}'
     - GET /restaurants/{id}/menu → first call reads DB and caches, subsequent call returns from cache quickly. Show response times or log entries if available.
     - Update menu (PUT or POST update) to trigger @CacheEvict; then GET to show fresh data.

  3) Place an order and show transactional outbox
     - POST /orders to order-service (port 8082). Example payload:
       curl -X POST http://localhost:8082/orders -H 'Content-Type: application/json' -d '{"restaurantId":"<id>","items":[{"menuItemId":"m1","qty":1}],"customer":{"name":"Alice"}}'
     - Show that the order row and an OutboxEvent row are persisted (quickly open DB or show logs). Example SQL: SELECT * FROM outbox_event WHERE published=false;
     - Show OutboxPoller logs publishing the event to RabbitMQ (tail .logs/order.log). The poller publishes to exchange swifteats.order.events and also forwards payment messages to queue payments.incoming.

  4) Payment mock consumption
     - Show payment-mock logs consuming payments.incoming and publishing a payment result event. (tail .logs/payment-mock.log)
     - Optionally show RabbitMQ management UI at http://localhost:15672 (guest/guest) to visualize exchanges/queues and live message rates.

  5) Tracking + SSE (use simulator to drive updates)
     - Start tracking services (tracking-ingest on 8084, tracking-query on 8085).
     - Start simulator in HTTP mode to emit driver position updates: POST /simulator/start with {"mode":"http","driverIds":["driver-1"],"intervalMs":1000}
     - Open SSE subscriber in terminal: curl -N -H "Accept: text/event-stream" http://localhost:8085/drivers/driver-1/location/stream
     - Show SSE stream printing location updates every second.
     - Check Redis key driver.location.driver-1 contains latest JSON (redis-cli or logs).

- What to show in logs during demo (important points to call out)
  - Order-service: creation SQL + OutboxEvent insert + OutboxPoller publish log lines.
  - Payment-mock: consumption of payments.incoming and emission of payment results.
  - Tracking-ingest: incoming POST writes to Redis key driver.location.{id} and publishes to driver.location.v1 exchange.
  - Tracking-query: SSE emit logs or the curl stream output (SSE events).

- Troubleshooting quick tips (if something fails live)
  - If apps cannot reach Postgres/Redis/RabbitMQ when run on host: ensure POSTGRES_HOST/REDIS_HOST/RABBITMQ_HOST are set to localhost (services run on host connecting to Docker mapped ports).
  - If Flyway errors on startup: either fix host env or set SPRING_FLYWAY_ENABLED=false for a quick demo and seed DB manually.
  - Use docker compose logs <service> and tail .logs/<service>.log for quick diagnosis.

6) Closing remarks & takeaways (last 30–60s)
- Reiterate: the demo validated end‑to‑end business logic: Redis caching behavior, reliable asynchronous delivery via transactional outbox + RabbitMQ, payment flow via a mock consumer, and realtime tracking via Redis + SSE driven by a simulator.
- Next steps: enable full Flyway migrations for production-like runs, add more observability (tracing, metrics), and add resilience (retry/backoff) for the OutboxPoller for stronger delivery guarantees.

Appendix — useful commands and curl snippets (copy/paste)
- docker compose up -d
- Check services:
  docker compose ps
- Catalog ping:
  curl http://localhost:8081/ping
- Create restaurant:
  curl -X POST http://localhost:8081/restaurants -H 'Content-Type: application/json' -d '{"name":"Demo Pizza","menu":[{"id":"m1","name":"Margherita","price":9.99}]}'
- Place an order:
  curl -X POST http://localhost:8082/orders -H 'Content-Type: application/json' -d '{"restaurantId":"<id>","items":[{"menuItemId":"m1","qty":1}],"customer":{"name":"Alice"}}'
- SSE stream:
  curl -N -H "Accept: text/event-stream" http://localhost:8085/drivers/driver-1/location/stream

File created: demo.md — use this as your script during the 9–10 minute presentation. Good luck with the demo.

Spoken script — exactly what to say (for a layman)

0:00–1:00 — Intro
"Hi, I’m going to show a small demo of a delivery platform we built called Swifteats. In one sentence: it’s a set of small services that work together to let restaurants publish menus, customers place orders, payments are processed, and drivers stream their live location. In this demo I will explain the design, tell you how we built it, highlight key decisions, and then run a short end-to-end flow so you can see it working live."

1:00–3:00 — Design & architecture (simple)
"At a high level the system has several parts that each do one job.
- One part stores restaurant menus and serves them quickly using a cache so reads are fast.
- One part accepts orders and makes sure order data and outgoing messages are stored reliably.
- A small payment mock service simulates a payment processor.
- A tracking part accepts driver GPS updates and another small part streams those updates to a browser or a terminal.

We use three infrastructure pieces you may have heard of: Postgres for permanent data, Redis for fast caching and storing the latest driver location, and RabbitMQ as a message broker so services can communicate without blocking each other. The pieces talk using simple HTTP calls and messages. This keeps the system reliable and easy to scale."

3:00–4:00 — Journey and interaction with assistant
"We started by planning the goals: make menu reads fast with caching, make ordering reliable with a transactional outbox, and build a simulator to exercise live tracking. I used a coding assistant to iterate quickly — it suggested tests, helped fix configuration issues, and guided how to run everything locally. That saved time and reduced mistakes."

4:00–5:00 — Key decisions and trade-offs (plain)
"The main choices were practical: instead of complex distributed transactions we used a transactional outbox. This avoids losing messages if a save succeeds but sending a message fails. For menu reads we used Redis caching — it’s fast but we must invalidate cache when menus change. We also decided to run services from the developer machine during the demo; that needs small environment settings so the apps connect to the local Docker containers. These are trade-offs for simplicity and reliability in a demo environment."

5:00–9:00 — Live demo narration (step-by-step, what to say while you run commands)
"Now I’ll run the live demo. I’ll keep my voice short while commands execute and point out what to watch.

Step A — Start infra:
Say: ‘I’ll start the infrastructure: Postgres, Redis and RabbitMQ.’
Run: docker compose up -d
Say while it starts: ‘These three services provide the database, cache, and message broker.’

Step B — Start services on my machine:
Say: ‘I’m starting the microservices. I run them with a few environment settings so they reach the Docker containers on my laptop.’
Start catalog service and say: ‘Catalog stores restaurants and menus. It uses Redis to cache menu results so reads are quick.’
Start order service and say: ‘Order service writes orders and also writes an outbox row. That outbox row will be picked up and sent to RabbitMQ so other services can react reliably.’
Start payment mock and tracking services and say briefly what each does.

Step C — Verify services are up:
Say: ‘I’ll ping the catalog service to show it’s running.’
Run: curl http://localhost:8081/ping
Say: ‘The service responded, so we can proceed.’

Step D — Create a restaurant and show caching:
Say: ‘I’ll create a demo restaurant with a menu.’
Run the POST for /restaurants.
After it returns say: ‘Now I will request the menu twice. The first request loads from the database and is cached. The second returns from Redis cache and is faster. If I update the menu, the cache is evicted so the next read shows the new menu.’

Step E — Place an order and show transactional outbox:
Say: ‘I’ll place an order for that menu item.’
Run the POST to /orders.
When done say: ‘The order and an outbox event were saved together in the database. The outbox poller will publish that event to RabbitMQ. This ensures we never lose the order event even if the message broker is temporarily unavailable.’

Step F — Payment processing (mock):
Say: ‘Payment service consumes the message and emits a result. I’ll show logs where the payment mock reads the payments queue and returns a payment result.’
Tail the payment mock log and point to consumption line.

Step G — Tracking and SSE stream with simulator:
Say: ‘Now I’ll start the simulator that sends driver location updates every second.’
Run the simulator start request.
Then say: ‘I’m opening a stream that listens to the driver updates. You will see JSON events printed for every location update.’
Open the SSE curl command and describe: ‘These events come from the tracking-query service which polls Redis for the latest location that the ingest service wrote. This gives a near-real-time view of the driver.’

During the demo, pause briefly at each log line and explain: ‘Here you can see the order outbox insert, the poller publishing to RabbitMQ, and the payment mock consuming the message. Here you can see the SSE event with the driver location.’

If anything fails say: ‘If a service can’t reach the database or the broker, check that the environment variables point to localhost because Docker exposes ports to my machine.’

8:45–9:00 — Wrap up and takeaways
Say: ‘That completes the demo. We showed the full flow: catalog caching, reliable order publishing with an outbox, payment processing, and realtime driver tracking via Redis and SSE. Next steps would be to add more observability and hardened retries for production. Thank you.’

Closing note
"If you want, I can shorten or simplify any section, or create speaker notes with the exact terminal commands on a single sheet for quick copy/paste."
