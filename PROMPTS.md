# SwiftEats — Phasewise Prompts & Guardrails

This file contains the full set of phasewise prompts and guardrails agreed for the SwiftEats project. Use this as the single source of truth for scaffolding and implementation tasks.

---

## GUARDRAILS (apply to all phases)

- Output ONLY files/code requested; no commentary when generating files.
- No TODOs/placeholders; all code must compile.
- Use Maven only (no Gradle).
- Use org.springframework.boot:spring-boot-starter-amqp for RabbitMQ.
- Compose must pass: `docker compose -f docker-compose.yml config -q`.
- `./mvnw -q -N validate` must succeed (no duplicate versions).
- Preflight: verify ports 5432, 6379, 5672, 15672 are free or allow overriding via env.

---

## Phase 0 — Project Structure Proposal (NO CODE)

**Goal:**
- Propose full repo layout for Java 21 + Spring Boot + Maven backend for SwiftEats.

**Output:**
1) Exact tree to be generated (root `pom.xml` packaging `pom`), modules: `gateway/`, `catalog-service/`, `order-service/`, `payment-mock-service/`, `tracking-ingest-service/`, `tracking-query-service/`, `simulator/`, `common/`; `docker-compose.yml`, `.env.example`, `README.md`, `PROJECT_STRUCTURE.md`, `ARCHITECTURE.md`, `API-SPECIFICATION.yml`, `CHAT_HISTORY.md`, `docs/`.
2) One-line purpose + default local port per module.
3) High-level data flows and which components scale independently.

**Rules:**
- No code. No pseudocode.
- Justify architecture pattern and list chosen infra (Postgres, Redis, RabbitMQ, Prometheus/Grafana optional).

**End:**
- Conclude with: `ACK: STRUCTURE-APPROVED` (only after your approval).

---

## Phase 1 — Baseline Runnable Scaffold (code + infra + docs)

**Goal:**
- Create multi-module Maven scaffold; RabbitMQ (not Kafka), Redis, Postgres.

**Hard requirements:**
- Each module exposes actuator health and `/ping`.
- `application.yml` reads env vars; structured JSON logging; graceful shutdown.

**Repo content to generate (print full file contents):**
- Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`).
- Root `pom.xml` (`<packaging>pom</packaging>`) with `pluginManagement` (surefire, failsafe, jacoco) and Spring Boot parent BOM.
- Child POMs for modules; use `org.springframework.boot:spring-boot-starter-amqp` for RabbitMQ.
- Minimal Spring Boot app in each module (health + `/ping`).
- `docker-compose.yml` (v3.9) with services: `postgres:16`, `redis:7`, `rabbitmq:3.13-management` using the healthcheck snippet below, user-defined bridge network, sensible ports. Include RabbitMQ healthcheck (and Redis optional healthcheck).
- `.env.example` with `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `REDIS_HOST`, `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASS`, etc.
- `README.md` with exact commands (plain commands; no markdown link placeholders):
  - `docker compose up -d`
  - `./mvnw -q -DskipTests package`
  - `./mvnw spring-boot:run` (per module)
  - `curl` health endpoints
- `PROJECT_STRUCTURE.md`, `ARCHITECTURE.md` (placeholder), `API-SPECIFICATION.yml` (skeleton), `CHAT_HISTORY.md` (placeholder), `.gitignore`.

**Self-check (plain commands):**
- Ensure `docker compose -f docker-compose.yml config -q` passes.
- Ensure `./mvnw -q -N validate` succeeds.

**RabbitMQ docker-compose healthcheck snippet (to be used in `docker-compose.yml`):**

```yaml
rabbitmq:
  image: rabbitmq:3.13-management
  ports: ["5672:5672","15672:15672"]
  healthcheck:
    test: ["CMD-SHELL","rabbitmq-diagnostics -q ping"]
    interval: 10s
    timeout: 5s
    retries: 10
  environment:
    RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-guest}
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS:-guest}
```

**Return:**
- Print repo tree then all files. End with: `ACK: SCAFFOLD-BUILT`.

---

## Phase 2 — Business Logic, Caching & Tests

**Goal:**
- Implement core domain: Catalog, Orders, Payment-Mock, Tracking ingest/query, Java simulator.

**Catalog:**
- Entities/DTOs/repos/services/controllers.
- Redis caching for `GET /restaurants/{id}/menu` with explicit cache keys, TTL, and invalidation on writes. Document TTLs and keys.

**Orders (OUTBOX clarification):**
- Persist transactional outbox row in the same DB transaction as the order insert.
- A reliable poller publishes outbox events to RabbitMQ exchange `swifteats.order.events` with routing keys `order.created`, `order.paid`.
- Consumers must be idempotent.
- Order create is idempotent; payment handled async.

**Payment-Mock:**
- Configurable latency/failure via env vars.

**Tracking:**
- `tracking-ingest-service` accepts driver GPS updates and publishes to RabbitMQ (exchange `driver.location.v1`).
- `tracking-query-service` reads latest location from Redis; SSE endpoint will stream in Phase 3.

**Simulator:**
- Java-based simulator configurable to hit ingest endpoint or publish to RabbitMQ; supports up to 50 drivers × 10 eps.

**Testing:**
- Unit tests (JUnit + Mockito), integration tests with Testcontainers for Postgres, Redis, RabbitMQ.
- JaCoCo coverage; target ≥80% for service layer.
- Flyway migrations (`V1__init.sql`) to seed test data.

**Docs:**
- Update `README.md` with `./mvnw -q clean verify` and simulator run steps.

**Self-verification:**
- Expected `./mvnw -q clean verify` → `SUCCESS`.
- List DB indexes and Redis keys/TTLs used.
- Confirm simulator can reach 50×10 eps locally.

**Pending sub-steps (Phase 2 priority)**

- A) Implement Catalog caching + tests (fast, isolated).
- B) Implement Outbox poller + full order-service integration tests (core transactional guarantee).
- C) Implement simulator multi-driver HTTP E2E (exercises tracking ingest + query SSE via Redis).

**Return:**
- Only updated/new files (full content). End with: `ACK: LOGIC-AND-TESTS-READY`.

---

## Phase 3 — Resilience, Streaming, and Performance Hardening

**Goal:**
- Add Resilience4j, transactional outbox publisher, SSE streaming, Redis-based latest location, Micrometer + Prometheus metrics, and dev JWT auth.

**Resilience:**
- Wrap payment calls with Resilience4j (timeouts, retries w/backoff+jitter, circuit breakers), idempotency keys, request correlation.
- Order Service implements transactional outbox and a reliable poller that publishes to RabbitMQ.

**Realtime tracking:**
- `tracking-query-service`:
  - SSE endpoint `/orders/{id}/track` streaming latest driver coords.
  - Latest driver location stored in Redis (simple key or GEO); small recent-path buffer in Redis.

**Performance:**
- Micrometer timers around browse endpoints; expose Prometheus metrics.

**Security:**
- Dev-only JWT auth for write endpoints; method-level role guards.

**Docs:**
- Update `ARCHITECTURE.md` with Mermaid diagram and technology justification (why Redis, RabbitMQ, Postgres).

**Return:**
- Only changed/new files. Provide sample env configs to simulate payment failures (e.g., `PAYMENT_FAILURE_RATE=0.3`, `PAYMENT_LATENCY_MS=500`) and a `curl` SSE example. End with: `ACK: RESILIENCE-AND-STREAMING-READY`.

---

## Phase 4 — Observability, CI, Images, Compose Profiles

**Goal:**
- Observability, GitHub Actions CI, Docker images per service, compose profiles, and operations docs.

**Observability:**
- Micrometer + Prometheus scrape config; starter Grafana dashboard JSON.

**API Docs:**
- springdoc-openapi per service; aggregate OpenAPI at `/openapi.yaml`; update `API-SPECIFICATION.yml`.

**CI:**
- GitHub Actions workflow: Temurin Java 21, cache Maven, `./mvnw -q clean verify`, optional Docker image build on push.

**Containers:**
- Dockerfile per service (slim JRE); compose profile `services` to run only app services (infra assumed up).

**Docs:**
- Complete `/docs`: `ARCHITECTURE.md`, `OPERATIONS.md`, `TESTING.md`, `SECURITY.md`, `CHANGELOG.md`, `CONTRIBUTING.md`.

**Spec deliverables (names EXACT):**
- `README.md`, `PROJECT_STRUCTURE.md`, `ARCHITECTURE.md`, `API-SPECIFICATION.yml`, `docker-compose.yml`, `CHAT_HISTORY.md`, tests+coverage.

**Return:**
- Only changed/new files. Self-verification:
  - `docker compose -f docker-compose.yml config -q` passes.
  - `./mvnw -q clean verify` passes.
  - Print JaCoCo report path. End with: `ACK: OBSERVABILITY-AND-CI-READY`.

---

## Phase 5 — Finalization & Build-from-Scratch Guide (docs + video script)

**Goal:**
- Final docs + video script content (only content to be added in video).

**Deliverables to produce/complete (print full Markdown files):**
- `README.md` (prereqs, one-command quickstart, troubleshooting, ports preflight, collaborator share steps).
- `PROJECT_STRUCTURE.md`, `ARCHITECTURE.md` (complete), `API-SPECIFICATION.yml` (complete), `docker-compose.yml`, `CHAT_HISTORY.md`.
- `/docs/VIDEO_GUIDE.md` — include only the script/content to add in the 8–10 minute video (design summary, architecture, demo steps, tests/coverage, trade-offs) and upload/share instructions (OneDrive + explicit recipients: `Dipen.mistry@talentica.com`, `Nilesh.Mallick@talentica.com`, `Sachin.Salunke@talentica.com`). DO NOT include actual video files.
- `/docs/OPERATIONS.md`, `/docs/SECURITY.md`, `/docs/TESTING.md`.

**Return:**
- Print full content of every requested Markdown file. Self-verification:
  - Confirm all required deliverables present with exact filenames and provide checklist mapping spec items to files. End with: `ACK: FINAL-DOCS-COMPLETE`.

---

## Phase V — Global Verifier & Auto-Fix (final pass)

**Goal:**
- Run final verification and auto-fix any failures.

**Checklist:**
1) Spec features & scale design present (order 500/min design, payment MOCK, browse P99 cache strategy, tracking simulator 50×10 eps).
2) Deliverables present with exact names.
3) Compose & build checks:
   - `docker compose -f docker-compose.yml config -q` → success.
   - `./mvnw -q -N validate` → SUCCESS.
   - `./mvnw -q clean verify` → SUCCESS.
4) Runtime checks (actuator health, sample endpoints, infra reachability).
5) Coverage (JaCoCo paths; >=80% service layer).

**If any check fails:**
- DIAGNOSTICS with root cause.
- Auto-fix minimal files and print corrected files (full contents).
- Re-run checklist and show expected outputs.

**Return:**
- End with: `ACK: VERIFIED`.

---

## Confirmations to provide before scaffolding or code generation

- Confirm Redis included? (default: yes)
- Confirm simulator language Java? (default: yes)
- Confirm realtime transport SSE? (default: yes)
- Confirm Prometheus + Grafana (+ optional Jaeger) in docker-compose? (default: include Prometheus + Grafana; Jaeger optional)
- Confirm you want me to proceed to Phase 0 → scaffold after you audit these updated prompts.

---

*End of PROMPTS.md*
