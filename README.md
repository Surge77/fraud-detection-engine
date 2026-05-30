# Fraud Detection Engine

Real-time transaction fraud detection engine — a **rules + velocity + weighted-score pipeline
that decides PASS/BLOCK in under 200ms**. The same class of system used by payment processors
to approve or block a transaction in milliseconds.

Built with production concerns first: idempotency, async decoupling, observability, clean
hexagonal architecture, and a test pyramid from pure unit tests up to Testcontainers integration.

[![CI](https://github.com/Surge77/fraud-detection-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/Surge77/fraud-detection-engine/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

## Architecture

Hexagonal (ports & adapters). The domain pipeline has **zero framework dependencies** and is
fully unit-testable without infrastructure.

```
                 ┌────────────┐   202 Accepted
   client ─POST─▶│   REST     │──────────────────────▶ caller polls GET /{id}
                 │ Controller │
                 └─────┬──────┘
                       │ publish
                 ┌─────▼───────────────┐
                 │ Kafka                │  transactions.incoming (3 partitions)
                 └─────┬───────────────┘
                       │ consume (manual ack, idempotent)
        ┌──────────────▼───────────────────────────────────────┐
        │  Fraud pipeline (domain — pure Java)                  │
        │                                                       │
        │  RulesEngine ─▶ VelocityChecker ─▶ RiskScorer ─▶ DecisionEngine
        │   (blacklist,    (Redis INCR+TTL)   (weighted)    (threshold)
        │    limit, geo)                                        │
        └───────┬──────────────────┬───────────────┬───────────┘
                │ audit             │ if BLOCK      │ if BLOCK
          ┌─────▼─────┐      ┌──────▼──────┐   ┌────▼──────────┐
          │ PostgreSQL│      │   Kafka     │   │  WebSocket    │
          │ audit_log │      │  .flagged   │   │  /topic/...   │
          └───────────┘      └─────────────┘   └───────────────┘
```

Layers: `api` → `application` → `domain` (model · pipeline · ports · exception) ← `infrastructure`
(kafka · redis · persistence · websocket) · `batch` · `config`.

## Scoring model

| Signal | Weight |
|---|---|
| Blacklisted merchant | 40 |
| High velocity (>10 tx / 5 min) | 30 |
| Amount exceeds daily limit | 20 |
| High-risk location | 10 |

Score is the sum of triggered weights, clamped to 100. **Score ≥ 75 → BLOCK**, otherwise PASS.
Weights and threshold are externalized in `application.yml` — tunable without a code deploy.

## Tech stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.3 |
| Messaging | Apache Kafka (Spring Kafka) |
| Cache / Velocity | Redis 7 (Lettuce) |
| Database | PostgreSQL 15 (Spring Data JPA + Flyway) |
| Batch | Spring Batch 5 |
| Real-time push | WebSocket + STOMP |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Micrometer + Prometheus + Grafana |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build / Runtime | Maven, Docker Compose |

## Getting started

### Prerequisites
- JDK 21, Maven 3.9+, Docker + Docker Compose.

### Run the stack
```bash
docker compose up -d          # Postgres, Redis, Kafka, Kafka UI, RedisInsight
mvn spring-boot:run           # starts the app on :8080 (local profile)
```

### Try it
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "11111111-1111-1111-1111-111111111111",
    "accountId": "acc_001",
    "amount": 100.00,
    "currency": "USD",
    "merchantId": "merch_999",
    "merchantName": "Clean Store",
    "location": "United States",
    "timestamp": "2026-05-30T12:00:00Z"
  }'
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Kafka UI: `http://localhost:8081`
- RedisInsight: `http://localhost:5540`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`

### Test
```bash
mvn test         # unit tests (no Docker)
mvn verify       # + Testcontainers integration tests (needs Docker)
```

## Roadmap

The engine is built in phases; see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detail.

1. **Foundation** — ingestion → persistence plumbing ✅
2. **Rules engine** — blacklist, daily limit, high-risk location
3. **Velocity + scoring** — Redis velocity, weighted scorer, decision engine
4. **Kafka** — fully async pipeline, idempotency, dead-letter topic
5. **WebSocket alerts** — live block-alert dashboard
6. **Spring Batch** — nightly fraud summary report
7. **Observability** — Micrometer metrics, Prometheus, structured JSON logs
8. **Testing** — unit + integration + load test (p99 < 200ms)

## License

[MIT](LICENSE)
