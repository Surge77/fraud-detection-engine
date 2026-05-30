package com.fraudengine.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Persisted record of a completed fraud evaluation. This is the read model
 * returned by lookups and consumed by the nightly batch report.
 *
 * @param transactionId evaluated transaction id
 * @param accountId     account that initiated the transaction
 * @param merchantId    merchant receiving the payment
 * @param amount        transaction amount
 * @param currency      ISO currency code
 * @param location      transaction location
 * @param riskScore     final risk score, 0..100
 * @param decision      PASS or BLOCK
 * @param reasons       reason codes behind the decision
 * @param createdAt     time the record was written
 */
public record AuditRecord(
        String transactionId,
        String accountId,
        String merchantId,
        BigDecimal amount,
        String currency,
        String location,
        int riskScore,
        Decision decision,
        List<String> reasons,
        Instant createdAt
) {
}
