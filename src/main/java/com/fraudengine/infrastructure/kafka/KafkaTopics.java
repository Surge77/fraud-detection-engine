package com.fraudengine.infrastructure.kafka;

/**
 * Kafka topic names used across the engine. Centralized so no topic string is
 * duplicated inline.
 */
public final class KafkaTopics {

    public static final String INCOMING = "transactions.incoming";
    public static final String FLAGGED = "transactions.flagged";
    public static final String INCOMING_DLT = "transactions.incoming.DLT";

    /**
     * Owned and published by ledger-engine, not by this service. Its payload is the
     * ledger's own domain event — {@code LedgerTransferMapper} translates it into a
     * {@link com.fraudengine.domain.model.TransactionRequest} rather than the ledger
     * conforming to our schema.
     */
    public static final String LEDGER_TRANSFERS_POSTED = "ledger.transfers.posted";

    private KafkaTopics() {
    }
}
