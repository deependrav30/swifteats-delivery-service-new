# Project Structure

Top-level modules:

- common/: shared DTOs and utilities used by all services.
- gateway/: API gateway / BFF that aggregates services and provides SSE endpoints to clients.
- catalog-service/: Restaurants and menu management; exposes menu read endpoints optimized with Redis caching.
- order-service/: Order lifecycle management, transactional outbox, and RabbitMQ publisher.
- payment-mock-service/: Mock payment API with configurable latency and failure rates for resilience testing.
- tracking-ingest-service/: Receives driver GPS updates and publishes to RabbitMQ exchange for processing.
- tracking-query-service/: Reads latest driver location from Redis and provides SSE streams for customer tracking.
- simulator/: Local Java simulator to generate driver telemetry and order load for local testing.

Supporting files:

- docker-compose.yml: local infra (Postgres, Redis, RabbitMQ) and networks/volumes.
- PROMPTS.md: phasewise prompts and guardrails.
- README.md: quickstart and run instructions.
- PROJECT_STRUCTURE.md: this file.
- ARCHITECTURE.md: system architecture and rationale (to be completed in Phase 3).
- API-SPECIFICATION.yml: OpenAPI skeleton (to be filled in later).
- CHAT_HISTORY.md: record of AI interactions and decisions.

Ports:
- gateway: 8080
- catalog-service: 8081
- order-service: 8082
- payment-mock-service: 8083
- tracking-ingest-service: 8084
- tracking-query-service: 8085
- simulator: 8086
