package com.fraudengine.infrastructure.redis;

import com.fraudengine.domain.ports.LocationRiskPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-set implementation of {@link LocationRiskPort} using {@code SISMEMBER}
 * against {@link RedisKeys#HIGH_RISK_LOCATIONS}.
 */
@Component
public class RedisLocationRiskAdapter implements LocationRiskPort {

    private final StringRedisTemplate redis;

    public RedisLocationRiskAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isHighRisk(String location) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(RedisKeys.HIGH_RISK_LOCATIONS, location));
    }
}
