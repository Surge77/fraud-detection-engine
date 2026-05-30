# Architecture

## Why these choices (interview framing)

| Decision | Rationale |
|---|---|
| **Kafka between API and pipeline** | Decouples ingestion from processing. The API returns `202` in single-digit ms even if the pipeline is slow or downstream is degraded. Buffers spikes. |
| **Redis `INCR` + TTL for velocity** | O(1) atomic counter. TTL auto-expires the window with no cleanup job. `SET NX EX` sets the TTL exactly once on first write, avoiding a sliding-expiry race. |
| **Flyway migrations** | Reproducible schema, versioned, no manual DDL drift across environments. |
| **Idempotency on `transactionId`** | Kafka is at-least-once. The consumer checks `audit_log` before processing so redelivery is a safe no-op. |
| **Weighted scorer with externalized weights** | Risk weights and threshold live in config — business can tune them without a code deploy. |
| **Spring Batch for the nightly report** | Built-in chunking, retry, skip, and restart semantics — more robust than a hand-rolled cron script. |
| **Hexagonal architecture** | Domain logic has zero framework deps, so the rules/velocity/scoring core is unit-testable in milliseconds without containers. |

## Layers

```
api/             Inbound adapters: REST controllers, STOMP WebSocket handlers, exception advice.
application/     Use-case orchestration. Wires domain pipeline stages and ports. Thin.
domain/
  model/         Immutable records (TransactionRequest, Transaction, FraudDecision, AuditRecord).
  pipeline/      RulesEngine, VelocityChecker, RiskScorer, DecisionEngine. Pure. No Spring.
  ports/         Outbound interfaces the domain depends on (AuditPort, VelocityPort, ...).
  exception/     FraudEngineException hierarchy.
infrastructure/  Outbound adapters implementing ports: kafka/, redis/, persistence/, websocket/.
batch/           Spring Batch FraudReportJob (reader → processor → writer).
config/          @Configuration: Kafka topics, Redis template, WebSocket broker, OpenAPI, metrics.
```

Dependency direction: `api`/`infrastructure` → `application` → `domain`. The domain never imports
outward. Adapters implement domain ports.

## The pipeline (request lifecycle)

1. **Ingest** — `POST /api/v1/transactions` validates the request (Bean Validation), publishes
   `TransactionRequest` to `transactions.incoming`, returns `202 {status: PROCESSING}`.
2. **Consume** — `@KafkaListener` (manual ack, `max-poll-records=10`). Idempotency check on
   `transactionId`; already-processed → skip.
3. **Rules (Stage A)** — blacklist (Redis set), daily-limit (Postgres `account_limits`),
   high-risk location (Redis set) → `List<RuleViolation>`.
4. **Velocity (Stage B)** — Redis `INCR fraud:velocity:{accountId}` with first-write TTL; count
   over threshold raises `HIGH_VELOCITY`.
5. **Score (Stage C)** — `RiskScorer` sums weights, clamps to 100 (pure function).
6. **Decide** — `DecisionEngine`: `score >= threshold ? BLOCK : PASS`.
7. **Persist** — write `audit_log` with score, decision, reasons (JSONB).
8. **React** — on BLOCK: publish to `transactions.flagged` and push a WebSocket alert.
9. **Poison messages** — unrecoverable failures route to `transactions.incoming.DLT`.

## Data model

- `audit_log` — one row per evaluated transaction (idempotency via unique `transaction_id`).
- `account_limits` — per-account daily spend ceiling.
- `fraud_reports` — one row per nightly batch run.

Redis keys: `fraud:blacklist:merchants` (set), `fraud:locations:high_risk` (set),
`fraud:velocity:{accountId}` (counter w/ TTL).

Kafka topics: `transactions.incoming` (3 partitions), `transactions.flagged` (1),
`transactions.incoming.DLT` (1).

## Performance target

End-to-end pipeline latency p99 < 200ms, validated by the load test in Phase 8.

Account daily limits are cached (Spring Cache, in-memory) since they change rarely and
are read on every transaction — this removes a database round trip from the hot path.

## Known limitations & trade-offs

Deliberate choices, called out so they are not mistaken for oversights:

- **Velocity active-accounts gauge** uses a `SCAN` over `fraud:velocity:*` on every Prometheus
  scrape. Fine at demo scale; at millions of keys this is O(N) per scrape and should be replaced
  with an approximate counter or a maintained set.
- **Velocity can over-count under Kafka redelivery.** If a redelivery arrives before the first
  attempt's audit row commits, the idempotency check (`existsByTransactionId`) misses and the
  Redis counter increments twice. The unique constraint on `audit_log.transaction_id` still
  prevents a duplicate decision; only the velocity metric is briefly inflated. A dedup store with
  TTL would close this fully.
- **Nightly report buckets by `created_at::date` in the server timezone.** Transactions near
  midnight across timezones could land in an adjacent day's report. Acceptable for a daily summary;
  a fixed reporting timezone would make it exact.
- **Admin auth is a shared-secret header**, not full identity/RBAC. Sufficient to gate the trigger
  endpoint for this scope; a real deployment would use Spring Security with proper authn/authz.
- **WebSocket alerts use the in-memory STOMP broker** — single-instance only. Multi-instance needs
  a broker relay or Redis pub/sub bridge (noted in `WebSocketConfig`).
