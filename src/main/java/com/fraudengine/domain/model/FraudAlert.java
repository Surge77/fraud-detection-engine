package com.fraudengine.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Real-time alert payload pushed to dashboards when a transaction is blocked.
 *
 * @param transactionId blocked transaction id
 * @param accountId     account involved
 * @param merchantId    merchant involved
 * @param amount        transaction amount
 * @param riskScore     final risk score
 * @param reasons       reason codes behind the block
 * @param timestamp     time the alert was raised
 */
public record FraudAlert(
        String transactionId,
        String accountId,
        String merchantId,
        BigDecimal amount,
        int riskScore,
        List<String> reasons,
        Instant timestamp
) {
}
