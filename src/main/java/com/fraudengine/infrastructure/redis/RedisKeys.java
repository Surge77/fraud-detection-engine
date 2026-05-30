package com.fraudengine.infrastructure.redis;

/**
 * Central registry of Redis key patterns. All keys follow {@code fraud:{entity}:{id}}
 * so there are no bare strings scattered across adapters.
 */
public final class RedisKeys {

    public static final String BLACKLIST_MERCHANTS = "fraud:blacklist:merchants";
    public static final String HIGH_RISK_LOCATIONS = "fraud:locations:high_risk";
    private static final String VELOCITY_PREFIX = "fraud:velocity:";

    private RedisKeys() {
    }

    /**
     * @param accountId account id
     * @return the velocity counter key for the account
     */
    public static String velocity(String accountId) {
        return VELOCITY_PREFIX + accountId;
    }
}
