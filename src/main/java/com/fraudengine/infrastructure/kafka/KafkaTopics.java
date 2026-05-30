package com.fraudengine.infrastructure.kafka;

/**
 * Kafka topic names used across the engine. Centralized so no topic string is
 * duplicated inline.
 */
public final class KafkaTopics {

    public static final String INCOMING = "transactions.incoming";
    public static final String FLAGGED = "transactions.flagged";
    public static final String INCOMING_DLT = "transactions.incoming.DLT";

    private KafkaTopics() {
    }
}
