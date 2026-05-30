package com.fraudengine.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound transaction to evaluate. {@code transactionId} doubles as the
 * idempotency key — the pipeline must produce the same decision for a given id
 * regardless of how many times it is delivered.
 *
 * @param transactionId client-supplied UUID, unique per logical transaction
 * @param accountId     account initiating the transaction
 * @param amount        transaction amount, must be positive
 * @param currency      ISO currency code
 * @param merchantId    merchant receiving the payment
 * @param merchantName  human-readable merchant name
 * @param location      country/region used for high-risk-location checks
 * @param timestamp     client-supplied event time
 */
public record TransactionRequest(
        @NotBlank String transactionId,
        @NotBlank String accountId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String merchantId,
        String merchantName,
        @NotBlank String location,
        @NotNull Instant timestamp
) {
}
