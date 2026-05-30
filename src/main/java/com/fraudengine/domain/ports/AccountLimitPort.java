package com.fraudengine.domain.ports;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Outbound port supplying an account's daily spend limit. Backed by the
 * {@code account_limits} table in the infrastructure layer.
 */
public interface AccountLimitPort {

    /**
     * @param accountId account to look up
     * @return the account's daily limit, or empty if no limit is configured
     */
    Optional<BigDecimal> dailyLimit(String accountId);
}
