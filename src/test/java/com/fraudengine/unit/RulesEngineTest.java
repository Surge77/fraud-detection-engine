package com.fraudengine.unit;

import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.pipeline.RulesEngine;
import com.fraudengine.domain.ports.AccountLimitPort;
import com.fraudengine.domain.ports.LocationRiskPort;
import com.fraudengine.domain.ports.MerchantBlacklistPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RulesEngineTest {

    @Mock
    private MerchantBlacklistPort blacklistPort;
    @Mock
    private AccountLimitPort accountLimitPort;
    @Mock
    private LocationRiskPort locationRiskPort;

    private RulesEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RulesEngine(blacklistPort, accountLimitPort, locationRiskPort);
        // Safe defaults — each test overrides only what it exercises.
        lenient().when(blacklistPort.isBlacklisted(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        lenient().when(locationRiskPort.isHighRisk(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        lenient().when(accountLimitPort.dailyLimit(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new BigDecimal("50000")));
    }

    private TransactionRequest req(String merchantId, String location, BigDecimal amount) {
        return new TransactionRequest(
                "tx", "acc_001", amount, "USD", merchantId, "name", location, Instant.now());
    }

    @Test
    void clean_transaction_raises_no_violations() {
        assertThat(engine.evaluate(req("merch_999", "United States", new BigDecimal("100"))))
                .isEmpty();
    }

    @Test
    void blacklisted_merchant_raises_violation() {
        org.mockito.Mockito.when(blacklistPort.isBlacklisted("merch_001")).thenReturn(true);
        assertThat(engine.evaluate(req("merch_001", "United States", new BigDecimal("100"))))
                .containsExactly(RuleViolation.BLACKLISTED_MERCHANT);
    }

    @Test
    void amount_over_daily_limit_raises_violation() {
        assertThat(engine.evaluate(req("merch_999", "United States", new BigDecimal("60000"))))
                .containsExactly(RuleViolation.AMOUNT_EXCEEDED);
    }

    @Test
    void amount_at_daily_limit_does_not_raise_violation() {
        assertThat(engine.evaluate(req("merch_999", "United States", new BigDecimal("50000"))))
                .isEmpty();
    }

    @Test
    void missing_account_limit_does_not_raise_violation() {
        org.mockito.Mockito.when(accountLimitPort.dailyLimit("acc_001")).thenReturn(Optional.empty());
        assertThat(engine.evaluate(req("merch_999", "United States", new BigDecimal("9999999"))))
                .isEmpty();
    }

    @Test
    void high_risk_location_raises_violation() {
        org.mockito.Mockito.when(locationRiskPort.isHighRisk("Nigeria")).thenReturn(true);
        assertThat(engine.evaluate(req("merch_999", "Nigeria", new BigDecimal("100"))))
                .containsExactly(RuleViolation.HIGH_RISK_LOCATION);
    }

    @Test
    void multiple_signals_raise_all_violations_in_order() {
        org.mockito.Mockito.when(blacklistPort.isBlacklisted("merch_001")).thenReturn(true);
        org.mockito.Mockito.when(locationRiskPort.isHighRisk("Iran")).thenReturn(true);
        assertThat(engine.evaluate(req("merch_001", "Iran", new BigDecimal("60000"))))
                .containsExactly(
                        RuleViolation.BLACKLISTED_MERCHANT,
                        RuleViolation.AMOUNT_EXCEEDED,
                        RuleViolation.HIGH_RISK_LOCATION);
    }
}
