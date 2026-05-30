package com.fraudengine.integration;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.infrastructure.persistence.AuditLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives a transaction through the real pipeline against live Postgres and Redis
 * and asserts the audit row is written.
 */
class FullPipelineIT extends AbstractIntegrationTest {

    @Autowired
    private TransactionEvaluationService evaluationService;

    @Autowired
    private AuditLogJpaRepository auditRepository;

    @Test
    void blacklisted_merchant_is_scored_and_audited() {
        String txId = UUID.randomUUID().toString();
        TransactionRequest request = new TransactionRequest(
                txId, "acc_001", new BigDecimal("100.00"), "USD",
                "merch_001", "Dodgy Store", "United States", Instant.now());

        FraudDecision decision = evaluationService.evaluate(request);

        assertThat(decision.reasons()).contains("BLACKLISTED_MERCHANT");
        assertThat(decision.riskScore()).isEqualTo(40);
        assertThat(decision.decision()).isEqualTo(Decision.PASS);
        assertThat(auditRepository.findByTransactionId(txId)).isPresent();
    }
}
