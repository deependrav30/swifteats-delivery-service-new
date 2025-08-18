# SwiftEats - Delivery Platform (Scaffold)

This repository contains a multi-module Java 21 + Spring Boot Maven scaffold for the SwiftEats delivery backend.

Quickstart (assumes Docker & Git clone):

1. Make the Maven wrapper executable (Unix/macOS):

   ```bash
   chmod +x mvnw
   ```

   Windows users: use `mvnw.cmd`.

2. Start infra:

   ```bash
   docker compose up -d
   ```

3. Build the project:

   ```bash
   ./mvnw -q -DskipTests package
   ```

4. Start a module (example: gateway):

   ```bash
   ./mvnw -q -pl gateway spring-boot:run
   ```

5. Check health:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

Notes:

- The repository includes the Maven Wrapper scripts (`mvnw`, `mvnw.cmd`) and `.mvn/wrapper/maven-wrapper.properties` to download the wrapper JAR at runtime if missing.
- If you encounter permission issues on Unix/macOS, run `chmod +x mvnw` before executing `./mvnw`.

See `PROMPTS.md` for the full project plan and phase instructions.

## Integration tests (catalog-service)

This project uses Testcontainers for integration tests. Tests in `catalog-service` start a Postgres container and seed the database using the SQL migration file instead of running Flyway during tests.

Prerequisites
- Docker running locally (Desktop or engine) and sufficient resources.
- Maven wrapper (`./mvnw`) executable.

Run catalog-service integration tests locally

- Run the module test suite (includes integration tests):

  ```bash
  ./mvnw -pl catalog-service test
  ```

- Run a single integration test:

  ```bash
  ./mvnw -Dtest=CatalogIntegrationTest -pl catalog-service test
  ```

Notes about Flyway and the test strategy
- Flyway auto-configuration is disabled for tests and the SQL migration file `src/main/resources/db/migration/V1__init.sql` is executed directly by tests. This avoids Flyway <-> database detection issues with certain Flyway versions.
- If you prefer Flyway-run migrations during tests, either pin the `flyway-core` version to one that supports the target Postgres or pin the Postgres container image to a version Flyway supports. I can make that change if you want Flyway enabled in tests.

Shared Testcontainers base
- Tests use a shared helper `catalog-service/src/test/java/com/swifteats/catalog/TestContainersBase.java`. Extend this base class in integration tests to get a single Postgres container and `runSql(...)` helper for seeding.

CI
- Ensure your CI runner supports Docker (GitHub-hosted runners do). A sample GitHub Actions workflow is included in `.github/workflows/integration-tests.yml` to run the module tests.