package com.fraudengine.domain.ports;

/**
 * Outbound port for the per-account transaction velocity counter. The
 * implementation must atomically increment the counter and apply the window
 * TTL on first creation.
 */
public interface VelocityPort {

    /**
     * Atomically increments the account's counter, setting the window TTL on the
     * first increment, and returns the resulting count.
     *
     * @param accountId     account whose velocity to record
     * @param windowSeconds TTL applied when the counter is first created
     * @return the count within the current window after this increment
     */
    long incrementAndCount(String accountId, int windowSeconds);
}
