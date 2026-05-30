# Contributing

## Development workflow

1. Branch from `main`: `feature/<short-desc>` or `fix/<short-desc>`.
2. Write the failing test first (TDD), then the minimum code to pass it.
3. Keep the domain layer free of framework imports — business logic lives in `domain/`.
4. Run the gate before pushing:
   ```bash
   mvn clean test      # unit
   mvn verify          # + integration (Docker required)
   ```
5. Open a PR. CI must be green.

## Conventions

- **Commits:** Conventional Commits — `feat(scope): ...`, `fix(scope): ...`, `test(...)`, `chore(...)`.
- **Redis keys:** `fraud:{entity}:{id}`.
- **Kafka topics / config:** constants or `@ConfigurationProperties`, never inline strings.
- **Errors:** RFC 7807 `ProblemDetail`; typed exceptions; never leak internals.
- **Logging:** SLF4J only; propagate `transactionId` via MDC.
- **File size:** 300 lines max; split by responsibility.
- **Public domain methods:** documented with Javadoc.

## Tests

- Unit: pure JUnit 5 + Mockito, no infrastructure.
- Integration: Testcontainers (Postgres, Redis, Kafka).
- Coverage target: 80%+ on new code; 100% on scoring/decision logic.
