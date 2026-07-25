package com.fraudengine.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.ports.AlertPort;
import com.fraudengine.domain.ports.FlaggedTransactionPublisher;
import com.fraudengine.infrastructure.kafka.ledger.LedgerTransferDecodeException;
import com.fraudengine.infrastructure.kafka.ledger.LedgerTransferListener;
import com.fraudengine.observability.FraudMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class LedgerTransferListenerTest {

    private static final String TRANSFER_JSON =
            "{\"transactionId\":42,\"from\":1001,\"to\":2002,\"amountMinor\":12345,\"currency\":\"USD\"}";

    @Mock
    private TransactionEvaluationService evaluationService;
    @Mock
    private FlaggedTransactionPublisher flaggedPublisher;
    @Mock
    private AlertPort alertPort;
    @Mock
    private Acknowledgment ack;

    private LedgerTransferListener listener;

    private static byte[] header(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static FraudDecision decision(Decision outcome) {
        return new FraudDecision("42", outcome, 90, List.of("high-velocity"), Instant.now());
    }

    @BeforeEach
    void setUp() {
        listener = new LedgerTransferListener(
                evaluationService, flaggedPublisher, alertPort,
                new FraudMetrics(new SimpleMeterRegistry()), new ObjectMapper());
    }

    @Test
    @DisplayName("evaluates a posted transfer and acknowledges it")
    void consume_postedTransfer_isEvaluated() {
        when(evaluationService.evaluate(any())).thenReturn(decision(Decision.PASS));

        listener.consume(TRANSFER_JSON, header("TRANSFER_POSTED"), "42", ack);

        verify(evaluationService).evaluate(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("publishes and alerts only when the decision is BLOCK")
    void consume_blockedTransfer_publishesAndAlerts() {
        when(evaluationService.evaluate(any())).thenReturn(decision(Decision.BLOCK));

        listener.consume(TRANSFER_JSON, header("TRANSFER_POSTED"), "42", ack);

        verify(flaggedPublisher).publish(any());
        verify(alertPort).push(any());
    }

    @Test
    @DisplayName("passing transfers are neither published nor alerted")
    void consume_passedTransfer_staysQuiet() {
        when(evaluationService.evaluate(any())).thenReturn(decision(Decision.PASS));

        listener.consume(TRANSFER_JSON, header("TRANSFER_POSTED"), "42", ack);

        verify(flaggedPublisher, never()).publish(any());
        verify(alertPort, never()).push(any());
    }

    @Test
    @DisplayName("skips reversals, which share the topic but carry no amount")
    void consume_reversalEvent_isSkippedNotFailed() {
        listener.consume("{\"transactionId\":9,\"reverses\":4}", header("REVERSAL_POSTED"), "9", ack);

        verify(evaluationService, never()).evaluate(any());
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("an undecodable payload propagates so the record is dead-lettered, not dropped")
    void consume_malformedPayload_throwsWithoutAcknowledging() {
        assertThatThrownBy(() -> listener.consume("not json", header("TRANSFER_POSTED"), "1", ack))
                .isInstanceOf(LedgerTransferDecodeException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    @DisplayName("a transfer missing its amount is dead-lettered rather than scored as zero")
    void consume_incompletePayload_throws() {
        String noAmount = "{\"transactionId\":42,\"from\":1001,\"to\":2002,\"currency\":\"USD\"}";

        assertThatThrownBy(() -> listener.consume(noAmount, header("TRANSFER_POSTED"), "42", ack))
                .isInstanceOf(LedgerTransferDecodeException.class);

        verify(evaluationService, never()).evaluate(any());
    }
}
