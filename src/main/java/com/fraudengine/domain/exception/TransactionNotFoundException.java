package com.fraudengine.domain.exception;

/**
 * Raised when a decision is requested for a transaction id that has not been
 * processed (or not yet processed, in the async flow).
 */
public class TransactionNotFoundException extends FraudEngineException {

    public TransactionNotFoundException(String transactionId) {
        super("No decision found for transaction: " + transactionId);
    }
}
