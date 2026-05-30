package com.fraudengine.infrastructure.seed;

import com.fraudengine.infrastructure.persistence.AccountLimitEntity;
import com.fraudengine.infrastructure.persistence.AccountLimitJpaRepository;
import com.fraudengine.infrastructure.redis.RedisKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Seeds reference data on startup. Idempotent: Redis {@code SADD} ignores
 * duplicates and account limits are keyed by primary key, so repeated runs are
 * safe.
 */
@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> BLACKLISTED_MERCHANTS =
            List.of("merch_001", "merch_002", "merch_003", "merch_004", "merch_005");

    private static final List<String> HIGH_RISK_LOCATIONS =
            List.of("Nigeria", "Belarus", "North Korea", "Iran", "Myanmar");

    private static final Map<String, BigDecimal> ACCOUNT_LIMITS = Map.ofEntries(
            Map.entry("acc_001", new BigDecimal("50000")),
            Map.entry("acc_002", new BigDecimal("10000")),
            Map.entry("acc_003", new BigDecimal("100000")),
            Map.entry("acc_004", new BigDecimal("25000")),
            Map.entry("acc_005", new BigDecimal("75000")),
            Map.entry("acc_006", new BigDecimal("5000")),
            Map.entry("acc_007", new BigDecimal("200000")),
            Map.entry("acc_008", new BigDecimal("15000")),
            Map.entry("acc_009", new BigDecimal("30000")),
            Map.entry("acc_010", new BigDecimal("1000")));

    private final StringRedisTemplate redis;
    private final AccountLimitJpaRepository accountLimitRepository;

    public DataSeeder(StringRedisTemplate redis, AccountLimitJpaRepository accountLimitRepository) {
        this.redis = redis;
        this.accountLimitRepository = accountLimitRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        redis.opsForSet().add(RedisKeys.BLACKLIST_MERCHANTS, BLACKLISTED_MERCHANTS.toArray(String[]::new));
        redis.opsForSet().add(RedisKeys.HIGH_RISK_LOCATIONS, HIGH_RISK_LOCATIONS.toArray(String[]::new));

        ACCOUNT_LIMITS.forEach((accountId, limit) -> {
            if (!accountLimitRepository.existsById(accountId)) {
                accountLimitRepository.save(new AccountLimitEntity(accountId, limit));
            }
        });

        log.info("Seeded {} blacklisted merchants, {} high-risk locations, {} account limits",
                BLACKLISTED_MERCHANTS.size(), HIGH_RISK_LOCATIONS.size(), ACCOUNT_LIMITS.size());
    }
}
