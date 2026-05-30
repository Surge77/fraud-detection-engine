package com.fraudengine.integration;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Redis-backed velocity window: the 11th transaction in the window
 * (max 10) raises HIGH_VELOCITY.
 */
class RedisVelocityIT extends AbstractIntegrationTest {

    private static final int OVER_LIMIT = 11;

    @Autowired
    private TransactionEvaluationService evaluationService;

    private TransactionRequest cleanRequest(String account) {
        return new TransactionRequest(
                UUID.randomUUID().toString(), account, new BigDecimal("10.00"), "USD",
                "merch_999", "Clean Store", "United States", Instant.now());
    }

    @Test
    void eleventh_transaction_in_window_triggers_high_velocity() {
        String account = "acc_007"; // high daily limit, avoids amount-exceeded noise
        FraudDecision last = null;
        for (int i = 0; i < OVER_LIMIT; i++) {
            last = evaluationService.evaluate(cleanRequest(account));
        }

        assertThat(last).isNotNull();
        assertThat(last.reasons()).contains("HIGH_VELOCITY");
    }
}
