package com.fraudengine.domain.exception;

/**
 * Raised when a transaction id has already been processed. Kafka delivers
 * at-least-once, so the consumer treats duplicates as a no-op rather than an
 * error; this type exists for the synchronous ingestion path.
 */
public class DuplicateTransactionException extends FraudEngineException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction already processed: " + transactionId);
    }
}
