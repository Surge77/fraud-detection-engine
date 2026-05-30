package com.fraudengine.domain.model;

/**
 * A discrete fraud signal raised by the rules engine. Each violation maps to a
 * configurable weight contributing to the final risk score.
 */
public enum RuleViolation {
    BLACKLISTED_MERCHANT,
    AMOUNT_EXCEEDED,
    HIGH_RISK_LOCATION,
    HIGH_VELOCITY
}
