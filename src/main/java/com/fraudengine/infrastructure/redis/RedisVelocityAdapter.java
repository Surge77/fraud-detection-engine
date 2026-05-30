package com.fraudengine.infrastructure.redis;

import com.fraudengine.domain.ports.VelocityPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis implementation of {@link VelocityPort}. Uses a Lua script so the
 * increment and first-write TTL happen atomically in a single round trip,
 * eliminating the race where a crash between INCR and EXPIRE would leave a
 * counter with no expiry (a permanent false velocity signal).
 */
@Component
public class RedisVelocityAdapter implements VelocityPort {

    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    public RedisVelocityAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incrementAndCount(String accountId, int windowSeconds) {
        Long count = redis.execute(
                INCREMENT_WITH_TTL,
                List.of(RedisKeys.velocity(accountId)),
                String.valueOf(windowSeconds));
        return count == null ? 0L : count;
    }
}
