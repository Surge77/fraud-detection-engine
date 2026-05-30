package com.fraudengine.unit;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.AuditRecord;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.model.VelocityResult;
import com.fraudengine.domain.pipeline.DecisionEngine;
import com.fraudengine.domain.pipeline.RiskScorer;
import com.fraudengine.domain.pipeline.RulesEngine;
import com.fraudengine.domain.pipeline.VelocityChecker;
import com.fraudengine.domain.ports.AuditPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEvaluationServiceTest {

    @Mock
    private RulesEngine rulesEngine;
    @Mock
    private VelocityChecker velocityChecker;
    @Mock
    private RiskScorer riskScorer;
    @Mock
    private DecisionEngine decisionEngine;
    @Mock
    private AuditPort auditPort;

    @Captor
    private ArgumentCaptor<AuditRecord> recordCaptor;

    private TransactionEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new TransactionEvaluationService(
                rulesEngine, velocityChecker, riskScorer, decisionEngine, auditPort);
        lenient().when(velocityChecker.record(anyString())).thenReturn(new VelocityResult(1, false));
        lenient().when(riskScorer.score(any())).thenReturn(0);
        lenient().when(decisionEngine.decide(org.mockito.ArgumentMatchers.anyInt())).thenReturn(Decision.PASS);
    }

    private TransactionRequest request(String txId) {
        return new TransactionRequest(
                txId, "acc_001", new BigDecimal("100.00"), "USD",
                "merch_999", "Clean Store", "United States", Instant.now());
    }

    @Test
    void returns_pass_with_no_reasons_for_clean_transaction() {
        when(rulesEngine.evaluate(any())).thenReturn(List.of());

        FraudDecision decision = service.evaluate(request("tx-1"));

        assertThat(decision.decision()).isEqualTo(Decision.PASS);
        assertThat(decision.riskScore()).isZero();
        assertThat(decision.reasons()).isEmpty();
    }

    @Test
    void adds_high_velocity_signal_when_velocity_exceeded() {
        when(rulesEngine.evaluate(any())).thenReturn(List.of(RuleViolation.BLACKLISTED_MERCHANT));
        when(velocityChecker.record(anyString())).thenReturn(new VelocityResult(11, true));
        when(riskScorer.score(any())).thenReturn(70);
        when(decisionEngine.decide(70)).thenReturn(Decision.PASS);

        FraudDecision decision = service.evaluate(request("tx-2"));

        assertThat(decision.reasons())
                .containsExactly("BLACKLISTED_MERCHANT", "HIGH_VELOCITY");
        assertThat(decision.riskScore()).isEqualTo(70);
    }

    @Test
    void persists_audit_record_with_score_and_decision() {
        when(rulesEngine.evaluate(any())).thenReturn(List.of(RuleViolation.BLACKLISTED_MERCHANT));
        when(velocityChecker.record(anyString())).thenReturn(new VelocityResult(11, true));
        when(riskScorer.score(any())).thenReturn(90);
        when(decisionEngine.decide(90)).thenReturn(Decision.BLOCK);

        service.evaluate(request("tx-3"));

        verify(auditPort).save(recordCaptor.capture());
        AuditRecord saved = recordCaptor.getValue();
        assertThat(saved.transactionId()).isEqualTo("tx-3");
        assertThat(saved.decision()).isEqualTo(Decision.BLOCK);
        assertThat(saved.riskScore()).isEqualTo(90);
    }
}
