package com.fraudengine.domain.pipeline;

import com.fraudengine.domain.model.VelocityResult;
import com.fraudengine.domain.ports.VelocityPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage B of the pipeline. Records a transaction against the account's velocity
 * counter and reports whether the windowed count exceeds the configured limit.
 */
public class VelocityChecker {

    private static final Logger log = LoggerFactory.getLogger(VelocityChecker.class);

    private final VelocityPort velocityPort;
    private final int windowSeconds;
    private final int maxTransactions;

    public VelocityChecker(VelocityPort velocityPort, int windowSeconds, int maxTransactions) {
        this.velocityPort = velocityPort;
        this.windowSeconds = windowSeconds;
        this.maxTransactions = maxTransactions;
    }

    /**
     * Records this transaction and evaluates the account's velocity.
     *
     * <p>The velocity store is a cache, not a source of truth. If it is
     * unavailable, the check fails open (no velocity signal) rather than failing
     * the whole evaluation — the merchant-blacklist and daily-limit rules, backed
     * by the database, still apply. This keeps the engine available for legitimate
     * traffic during a Redis outage instead of dead-lettering every transaction.
     *
     * @param accountId account initiating the transaction
     * @return the count and whether it exceeds the configured maximum; a zero,
     *         non-exceeded result when the velocity store is unavailable
     */
    public VelocityResult record(String accountId) {
        try {
            long count = velocityPort.incrementAndCount(accountId, windowSeconds);
            return new VelocityResult(count, count > maxTransactions);
        } catch (RuntimeException e) {
            log.warn("Velocity store unavailable for account {} — failing open (no velocity signal): {}",
                    accountId, e.getMessage());
            return new VelocityResult(0, false);
        }
    }
}
