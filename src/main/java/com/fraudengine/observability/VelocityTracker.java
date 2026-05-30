package com.fraudengine.observability;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Reports the number of accounts with an active velocity window. Backs the
 * {@code fraud.velocity.active_accounts} gauge.
 */
@Component
public class VelocityTracker {

    private static final String VELOCITY_PATTERN = "fraud:velocity:*";
    private static final int SCAN_BATCH = 500;

    private final StringRedisTemplate redis;

    public VelocityTracker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Counts active velocity keys. Uses a cursor-based SCAN (never KEYS, which
     * would block Redis) so the gauge is safe to poll. Expired windows drop out
     * automatically because the underlying keys carry a TTL.
     *
     * @return number of accounts with an active velocity window
     */
    public long activeAccountCount() {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions().match(VELOCITY_PATTERN).count(SCAN_BATCH).build();
        try (Cursor<byte[]> cursor = redis.executeWithStickyConnection(
                connection -> connection.keyCommands().scan(options))) {
            while (cursor != null && cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }
}
