package com.fraudengine.application;

import com.fraudengine.domain.model.AuditRecord;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.pipeline.RulesEngine;
import com.fraudengine.domain.ports.AuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrates evaluation of a transaction and persists the outcome.
 *
 * <p>Phase 2 runs the rules engine synchronously and surfaces the resulting
 * violations as decision reasons. Scoring and the BLOCK/PASS decision are wired
 * in Phase 3; until then the decision remains PASS.
 */
@Service
public class TransactionEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionEvaluationService.class);

    private final RulesEngine rulesEngine;
    private final AuditPort auditPort;

    public TransactionEvaluationService(RulesEngine rulesEngine, AuditPort auditPort) {
        this.rulesEngine = rulesEngine;
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
        List<RuleViolation> violations = rulesEngine.evaluate(request);
        List<String> reasons = violations.stream().map(Enum::name).toList();

        FraudDecision decision = new FraudDecision(
                request.transactionId(), Decision.PASS, 0, reasons, decidedAt);

        auditPort.save(new AuditRecord(
                request.transactionId(), request.accountId(), request.merchantId(),
                request.amount(), request.currency(), request.location(),
                decision.riskScore(), decision.decision(), decision.reasons(), decidedAt));

        log.info("Evaluated transaction {} -> {} (score {}, reasons {})",
                request.transactionId(), decision.decision(), decision.riskScore(), reasons);
        return decision;
    }
}
