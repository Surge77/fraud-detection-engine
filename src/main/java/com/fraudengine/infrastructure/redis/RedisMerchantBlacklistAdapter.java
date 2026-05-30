package com.fraudengine.infrastructure.redis;

import com.fraudengine.domain.ports.MerchantBlacklistPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-set implementation of {@link MerchantBlacklistPort} using {@code SISMEMBER}
 * against {@link RedisKeys#BLACKLIST_MERCHANTS}.
 */
@Component
public class RedisMerchantBlacklistAdapter implements MerchantBlacklistPort {

    private final StringRedisTemplate redis;

    public RedisMerchantBlacklistAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isBlacklisted(String merchantId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(RedisKeys.BLACKLIST_MERCHANTS, merchantId));
    }
}
