package com.fraudengine.infrastructure.kafka.ledger;

/**
 * Wire shape of a {@code TRANSFER_POSTED} event published by ledger-engine. This
 * mirrors that service's outbox payload exactly and lives in the inbound adapter —
 * the domain must not learn the ledger's vocabulary.
 *
 * @param transactionId ledger transaction id, unique within ledger-engine
 * @param from          debited account — the party whose behavior is scored
 * @param to            credited account
 * @param amountMinor   amount in minor units (cents), as the ledger stores it
 * @param currency      ISO currency code
 */
public record LedgerTransferEvent(
        Long transactionId,
        Long from,
        Long to,
        Long amountMinor,
        String currency) {
}
