package com.fraudengine.infrastructure.persistence;

import com.fraudengine.config.CacheConfig;
import com.fraudengine.domain.ports.AccountLimitPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * JPA-backed implementation of {@link AccountLimitPort} reading from the
 * {@code account_limits} table.
 */
@Component
public class AccountLimitAdapter implements AccountLimitPort {

    private final AccountLimitJpaRepository repository;

    public AccountLimitAdapter(AccountLimitJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(CacheConfig.ACCOUNT_LIMITS)
    public Optional<BigDecimal> dailyLimit(String accountId) {
        return repository.findById(accountId).map(AccountLimitEntity::getDailyLimit);
    }
}
