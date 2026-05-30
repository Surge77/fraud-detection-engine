package com.fraudengine.infrastructure.kafka;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudAlert;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.AlertPort;
import com.fraudengine.domain.ports.FlaggedTransactionPublisher;
import com.fraudengine.observability.FraudMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes incoming transactions, runs the full evaluation pipeline, and emits
 * blocked decisions downstream. Uses manual acknowledgment so the offset is
 * committed only after successful processing; on failure the configured error
 * handler retries and ultimately routes to the dead-letter topic.
 */
@Component
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);
    private static final String MDC_TRANSACTION_ID = "transactionId";

    private final TransactionEvaluationService evaluationService;
    private final FlaggedTransactionPublisher flaggedPublisher;
    private final AlertPort alertPort;
    private final FraudMetrics metrics;

    public TransactionConsumer(TransactionEvaluationService evaluationService,
                               FlaggedTransactionPublisher flaggedPublisher,
                               AlertPort alertPort,
                               FraudMetrics metrics) {
        this.evaluationService = evaluationService;
        this.flaggedPublisher = flaggedPublisher;
        this.alertPort = alertPort;
        this.metrics = metrics;
    }

    @KafkaListener(topics = KafkaTopics.INCOMING, groupId = "fraud-engine")
    public void consume(TransactionRequest request, Acknowledgment ack) {
        MDC.put(MDC_TRANSACTION_ID, request.transactionId());
        try {
            FraudDecision decision = metrics.time(() -> evaluationService.evaluate(request));
            metrics.countDecision(decision.decision());
            if (decision.decision() == Decision.BLOCK) {
                flaggedPublisher.publish(decision);
                alertPort.push(toAlert(request, decision));
            }
            ack.acknowledge();
            log.debug("Acknowledged transaction {}", request.transactionId());
        } finally {
            MDC.remove(MDC_TRANSACTION_ID);
        }
    }

    private FraudAlert toAlert(TransactionRequest request, FraudDecision decision) {
        return new FraudAlert(
                request.transactionId(), request.accountId(), request.merchantId(),
                request.amount(), decision.riskScore(), decision.reasons(), decision.decidedAt());
    }
}
