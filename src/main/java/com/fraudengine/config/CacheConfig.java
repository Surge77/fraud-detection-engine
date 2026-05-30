package com.fraudengine.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's caching abstraction. Backed by the default in-memory
 * ConcurrentMapCacheManager — sufficient for reference data (account limits)
 * that changes rarely and is read on every transaction.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ACCOUNT_LIMITS = "accountLimits";
}
