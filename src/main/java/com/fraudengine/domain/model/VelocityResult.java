package com.fraudengine.domain.model;

/**
 * Outcome of a velocity check for one transaction.
 *
 * @param count    transaction count in the current window after recording this one
 * @param exceeded true if the count is over the configured maximum
 */
public record VelocityResult(long count, boolean exceeded) {
}
