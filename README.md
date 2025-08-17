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