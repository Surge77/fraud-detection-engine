package com.fraudengine.unit;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.AuditRecord;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.AuditPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionEvaluationServiceTest {

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
    void returns_pass_with_zero_score_in_phase1() {
        TransactionEvaluationService service = new TransactionEvaluationService(auditPort);

        FraudDecision decision = service.evaluate(request("tx-1"));

        assertThat(decision.decision()).isEqualTo(Decision.PASS);
        assertThat(decision.riskScore()).isZero();
        assertThat(decision.reasons()).isEmpty();
        assertThat(decision.transactionId()).isEqualTo("tx-1");
    }

    @Test
    void persists_audit_record_for_evaluated_transaction() {
        TransactionEvaluationService service = new TransactionEvaluationService(auditPort);

        service.evaluate(request("tx-2"));

        verify(auditPort).save(recordCaptor.capture());
        AuditRecord saved = recordCaptor.getValue();
        assertThat(saved.transactionId()).isEqualTo("tx-2");
        assertThat(saved.decision()).isEqualTo(Decision.PASS);
        assertThat(saved.accountId()).isEqualTo("acc_001");
    }
}
