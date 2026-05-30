package com.fraudengine.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Final result returned to callers and persisted to the audit log.
 *
 * @param transactionId the evaluated transaction's id
 * @param decision      PASS or BLOCK
 * @param riskScore     aggregate weighted score, clamped to 0..100
 * @param reasons       human-readable reason codes behind the decision
 * @param decidedAt     time the decision was produced
 */
public record FraudDecision(
        String transactionId,
        Decision decision,
        int riskScore,
        List<String> reasons,
        Instant decidedAt
) {
    public FraudDecision {
        reasons = List.copyOf(reasons);
    }
}
