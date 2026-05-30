package com.fraudengine.domain.exception;

/**
 * Raised when a request to a protected endpoint is missing or presents an
 * invalid credential.
 */
public class UnauthorizedException extends FraudEngineException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
