# Contributing

Developer workflow

- Ensure Docker is running locally before running integration tests.
- Make the Maven wrapper executable: `chmod +x mvnw`

Run tests

- Run module tests:

```bash
./mvnw -pl catalog-service test
```

Test strategy notes

- Integration tests use Testcontainers and seed the database by executing SQL migration scripts directly. Flyway auto-configuration is disabled for tests to avoid compatibility problems.
- To enable Flyway in tests, pin `flyway-core` or the Postgres image; see `README.md` for details.
