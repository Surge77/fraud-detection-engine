package com.fraudengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized, business-tunable fraud parameters bound from the {@code fraud:}
 * config prefix. Changing weights or the threshold requires only a config
 * change, not a code deploy.
 *
 * @param weights   per-signal score contributions
 * @param threshold score at or above which a transaction is blocked
 * @param velocity  velocity-window configuration
 */
@ConfigurationProperties(prefix = "fraud")
public record FraudProperties(
        Weights weights,
        int threshold,
        Velocity velocity
) {
    /**
     * Score contribution for each fraud signal.
     */
    public record Weights(
            int blacklistedMerchant,
            int highVelocity,
            int amountExceeded,
            int highRiskLocation
    ) {
    }

    /**
     * Velocity window settings.
     *
     * @param windowSeconds   sliding window length in seconds
     * @param maxTransactions count above which HIGH_VELOCITY is raised
     */
    public record Velocity(
            int windowSeconds,
            int maxTransactions
    ) {
    }
}
