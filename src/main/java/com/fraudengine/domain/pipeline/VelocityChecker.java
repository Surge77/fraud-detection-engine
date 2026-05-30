package com.fraudengine.domain.pipeline;

import com.fraudengine.domain.model.VelocityResult;
import com.fraudengine.domain.ports.VelocityPort;

/**
 * Stage B of the pipeline. Records a transaction against the account's velocity
 * counter and reports whether the windowed count exceeds the configured limit.
 */
public class VelocityChecker {

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
     * @param accountId account initiating the transaction
     * @return the count and whether it exceeds the configured maximum
     */
    public VelocityResult record(String accountId) {
        long count = velocityPort.incrementAndCount(accountId, windowSeconds);
        return new VelocityResult(count, count > maxTransactions);
    }
}
