package com.fraudengine.application;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full fraud pipeline for a transaction: rules → velocity →
 * scoring → decision, then persists the audit record.
 */
@Service
public class TransactionEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionEvaluationService.class);

    private final RulesEngine rulesEngine;
    private final VelocityChecker velocityChecker;
    private final RiskScorer riskScorer;
    private final DecisionEngine decisionEngine;
    private final AuditPort auditPort;

    public TransactionEvaluationService(RulesEngine rulesEngine,
                                        VelocityChecker velocityChecker,
                                        RiskScorer riskScorer,
                                        DecisionEngine decisionEngine,
                                        AuditPort auditPort) {
        this.rulesEngine = rulesEngine;
        this.velocityChecker = velocityChecker;
        this.riskScorer = riskScorer;
        this.decisionEngine = decisionEngine;
        this.auditPort = auditPort;
    }

    /**
     * Runs the full pipeline for a transaction and writes an audit record.
     *
     * @param request the transaction to evaluate
     * @return the fraud decision
     */
    public FraudDecision evaluate(TransactionRequest request) {
        Instant decidedAt = Instant.now();

        List<RuleViolation> signals = new ArrayList<>(rulesEngine.evaluate(request));
        VelocityResult velocity = velocityChecker.record(request.accountId());
        if (velocity.exceeded()) {
            signals.add(RuleViolation.HIGH_VELOCITY);
        }

        int score = riskScorer.score(signals);
        Decision decision = decisionEngine.decide(score);
        List<String> reasons = signals.stream().map(Enum::name).toList();

        auditPort.save(new AuditRecord(
                request.transactionId(), request.accountId(), request.merchantId(),
                request.amount(), request.currency(), request.location(),
                score, decision, reasons, decidedAt));

        log.info("Evaluated transaction {} -> {} (score {}, velocity {}, reasons {})",
                request.transactionId(), decision, score, velocity.count(), reasons);
        return new FraudDecision(request.transactionId(), decision, score, reasons, decidedAt);
    }
}
