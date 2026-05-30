package com.fraudengine.unit;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.AuditRecord;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.pipeline.RulesEngine;
import com.fraudengine.domain.ports.AuditPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEvaluationServiceTest {

    @Mock
    private RulesEngine rulesEngine;

    @Mock
    private AuditPort auditPort;

    @Captor
    private ArgumentCaptor<AuditRecord> recordCaptor;

    private TransactionRequest request(String txId) {
        return new TransactionRequest(
                txId, "acc_001", new BigDecimal("100.00"), "USD",
                "merch_999", "Clean Store", "United States", Instant.now());
    }

    @Test
    void returns_pass_with_no_reasons_for_clean_transaction() {
        when(rulesEngine.evaluate(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        TransactionEvaluationService service = new TransactionEvaluationService(rulesEngine, auditPort);

        FraudDecision decision = service.evaluate(request("tx-1"));

        assertThat(decision.decision()).isEqualTo(Decision.PASS);
        assertThat(decision.riskScore()).isZero();
        assertThat(decision.reasons()).isEmpty();
    }

    @Test
    void surfaces_rule_violations_as_reasons() {
        when(rulesEngine.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(RuleViolation.BLACKLISTED_MERCHANT));
        TransactionEvaluationService service = new TransactionEvaluationService(rulesEngine, auditPort);

        FraudDecision decision = service.evaluate(request("tx-2"));

        assertThat(decision.reasons()).containsExactly("BLACKLISTED_MERCHANT");
    }

    @Test
    void persists_audit_record_for_evaluated_transaction() {
        when(rulesEngine.evaluate(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        TransactionEvaluationService service = new TransactionEvaluationService(rulesEngine, auditPort);

        service.evaluate(request("tx-3"));

        verify(auditPort).save(recordCaptor.capture());
        AuditRecord saved = recordCaptor.getValue();
        assertThat(saved.transactionId()).isEqualTo("tx-3");
        assertThat(saved.decision()).isEqualTo(Decision.PASS);
        assertThat(saved.accountId()).isEqualTo("acc_001");
    }
}
