package com.fraudengine.infrastructure.kafka.ledger;

/**
 * Raised when a ledger event cannot be decoded or translated. Propagates so the
 * container's error handler retries and ultimately dead-letters the record — a
 * transfer we cannot read must never be treated as evaluated.
 */
public class LedgerTransferDecodeException extends RuntimeException {

    public LedgerTransferDecodeException(Throwable cause) {
        super("failed to decode ledger transfer event", cause);
    }
}
