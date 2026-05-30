package com.fraudengine.domain.pipeline;

import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.AccountLimitPort;
import com.fraudengine.domain.ports.LocationRiskPort;
import com.fraudengine.domain.ports.MerchantBlacklistPort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage A of the pipeline. Applies the static fraud rules — merchant blacklist,
 * daily-limit breach, and high-risk location — and returns the violations
 * raised. Pure domain logic; data access is delegated to ports.
 */
public class RulesEngine {

    private final MerchantBlacklistPort blacklistPort;
    private final AccountLimitPort accountLimitPort;
    private final LocationRiskPort locationRiskPort;

    public RulesEngine(MerchantBlacklistPort blacklistPort,
                       AccountLimitPort accountLimitPort,
                       LocationRiskPort locationRiskPort) {
        this.blacklistPort = blacklistPort;
        this.accountLimitPort = accountLimitPort;
        this.locationRiskPort = locationRiskPort;
    }

    /**
     * Evaluates all static rules for a transaction.
     *
     * @param request the transaction to evaluate
     * @return the violations raised, in evaluation order (never null)
     */
    public List<RuleViolation> evaluate(TransactionRequest request) {
        List<RuleViolation> violations = new ArrayList<>();
        if (isBlacklistedMerchant(request.merchantId())) {
            violations.add(RuleViolation.BLACKLISTED_MERCHANT);
        }
        if (exceedsDailyLimit(request.accountId(), request.amount())) {
            violations.add(RuleViolation.AMOUNT_EXCEEDED);
        }
        if (isHighRiskLocation(request.location())) {
            violations.add(RuleViolation.HIGH_RISK_LOCATION);
        }
        return violations;
    }

    /**
     * @param merchantId merchant to check
     * @return true if the merchant is blacklisted
     */
    public boolean isBlacklistedMerchant(String merchantId) {
        return blacklistPort.isBlacklisted(merchantId);
    }

    /**
     * @param accountId account initiating the transaction
     * @param amount    transaction amount
     * @return true if the amount exceeds the account's configured daily limit;
     *         false when no limit is configured
     */
    public boolean exceedsDailyLimit(String accountId, BigDecimal amount) {
        return accountLimitPort.dailyLimit(accountId)
                .map(limit -> amount.compareTo(limit) > 0)
                .orElse(false);
    }

    /**
     * @param location transaction location
     * @return true if the location is classified high-risk
     */
    public boolean isHighRiskLocation(String location) {
        return locationRiskPort.isHighRisk(location);
    }
}
