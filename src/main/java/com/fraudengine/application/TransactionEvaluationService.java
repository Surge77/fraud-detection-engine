package com.fraudengine.application;

import com.fraudengine.domain.model.AuditRecord;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.AuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates evaluation of a transaction and persists the outcome.
 *
 * <p>Phase 1 returns a hardcoded PASS to prove the ingestion-to-persistence
 * plumbing. Later phases replace the body with the full rules → velocity →
 * scorer → decision pipeline.
 */
@Service
public class TransactionEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionEvaluationService.class);

    private final AuditPort auditPort;

    public TransactionEvaluationService(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    /**
     * Evaluates a transaction and writes an audit record.
     *
     * @param request the transaction to evaluate
     * @return the fraud decision
     */
    public FraudDecision evaluate(TransactionRequest request) {
        Instant decidedAt = Instant.now();
        FraudDecision decision = new FraudDecision(
                request.transactionId(), Decision.PASS, 0, List.of(), decidedAt);

        auditPort.save(new AuditRecord(
                request.transactionId(), request.accountId(), request.merchantId(),
                request.amount(), request.currency(), request.location(),
                decision.riskScore(), decision.decision(), decision.reasons(), decidedAt));

        log.info("Evaluated transaction {} -> {} (score {})",
                request.transactionId(), decision.decision(), decision.riskScore());
        return decision;
    }
}
