package com.fraudengine.domain.exception;

/**
 * Base type for all domain-level failures in the fraud engine. Allows callers
 * to catch every engine-specific error with a single handler while still
 * distinguishing concrete subtypes.
 */
public class FraudEngineException extends RuntimeException {

    public FraudEngineException(String message) {
        super(message);
    }

    public FraudEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
