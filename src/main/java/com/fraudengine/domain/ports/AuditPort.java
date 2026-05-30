package com.fraudengine.domain.ports;

import com.fraudengine.domain.model.AuditRecord;

import java.util.Optional;

/**
 * Outbound port for persisting and retrieving fraud-evaluation audit records.
 * The domain depends only on this interface; the JPA adapter lives in the
 * infrastructure layer.
 */
public interface AuditPort {

    /**
     * Persists a completed audit record.
     *
     * @param record the record to store
     */
    void save(AuditRecord record);

    /**
     * Looks up a previously stored decision by transaction id.
     *
     * @param transactionId the transaction id to find
     * @return the audit record if present, otherwise empty
     */
    Optional<AuditRecord> findByTransactionId(String transactionId);

    /**
     * Tests whether a transaction has already been processed. Used for
     * idempotency on the at-least-once Kafka delivery path.
     *
     * @param transactionId the transaction id to check
     * @return true if a record already exists
     */
    boolean existsByTransactionId(String transactionId);
}
