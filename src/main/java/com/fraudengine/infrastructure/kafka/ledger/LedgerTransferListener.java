package com.fraudengine.infrastructure.kafka.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudAlert;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.AlertPort;
import com.fraudengine.domain.ports.FlaggedTransactionPublisher;
import com.fraudengine.infrastructure.kafka.KafkaTopics;
import com.fraudengine.observability.FraudMetrics;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes transfers posted by ledger-engine and runs them through the same
 * evaluation pipeline as merchant traffic, after translation by
 * {@link LedgerTransferMapper}.
 *
 * <p>Reads the raw JSON payload rather than a typed deserializer: the ledger owns
 * this schema, so decoding is done here where the mapping is, and a shape change
 * upstream surfaces as a parse failure on this topic alone.
 */
@Component
public class LedgerTransferListener {

    private static final Logger log = LoggerFactory.getLogger(LedgerTransferListener.class);
    private static final String MDC_TRANSACTION_ID = "transactionId";

    /** Only posted transfers carry an amount; reversals are a different shape. */
    private static final String TRANSFER_POSTED = "TRANSFER_POSTED";
    private static final String EVENT_TYPE_HEADER = "event-type";

    private final TransactionEvaluationService evaluationService;
    private final FlaggedTransactionPublisher flaggedPublisher;
    private final AlertPort alertPort;
    private final FraudMetrics metrics;
    private final ObjectMapper objectMapper;

    public LedgerTransferListener(TransactionEvaluationService evaluationService,
                                  FlaggedTransactionPublisher flaggedPublisher,
                                  AlertPort alertPort,
                                  FraudMetrics metrics,
                                  ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.flaggedPublisher = flaggedPublisher;
        this.alertPort = alertPort;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = KafkaTopics.LEDGER_TRANSFERS_POSTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "ledgerTransferListenerContainerFactory")
    public void consume(String payload,
                        @Header(name = EVENT_TYPE_HEADER, required = false) byte[] eventType,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        Acknowledgment ack) {
        String type = eventType == null ? TRANSFER_POSTED : new String(eventType);
        if (!TRANSFER_POSTED.equals(type)) {
            // Reversals and future event types share the topic but carry no amount —
            // acknowledging skips them without poisoning the partition.
            log.debug("Skipping non-transfer ledger event type={} key={}", type, key);
            ack.acknowledge();
            return;
        }

        TransactionRequest request = translate(payload);
        MDC.put(MDC_TRANSACTION_ID, request.transactionId());
        try {
            FraudDecision decision = metrics.time(() -> evaluationService.evaluate(request));
            metrics.countDecision(decision.decision());
            if (decision.decision() == Decision.BLOCK) {
                flaggedPublisher.publish(decision);
                alertPort.push(toAlert(request, decision));
            }
            ack.acknowledge();
            log.debug("Acknowledged ledger transfer {}", request.transactionId());
        } finally {
            MDC.remove(MDC_TRANSACTION_ID);
        }
    }

    private TransactionRequest translate(String payload) {
        try {
            LedgerTransferEvent event = objectMapper.readValue(payload, LedgerTransferEvent.class);
            return LedgerTransferMapper.toTransactionRequest(event, Instant.now());
        } catch (Exception e) {
            // Rethrown so the configured error handler retries and then routes to the
            // dead-letter topic — swallowing it here would silently drop a real transfer.
            throw new LedgerTransferDecodeException(e);
        }
    }

    private FraudAlert toAlert(TransactionRequest request, FraudDecision decision) {
        return new FraudAlert(
                request.transactionId(), request.accountId(), request.merchantId(),
                request.amount(), decision.riskScore(), decision.reasons(), decision.decidedAt());
    }
}
