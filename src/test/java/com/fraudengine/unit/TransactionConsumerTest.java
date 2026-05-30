package com.fraudengine.unit;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.FlaggedTransactionPublisher;
import com.fraudengine.infrastructure.kafka.TransactionConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    @Mock
    private TransactionEvaluationService evaluationService;
    @Mock
    private FlaggedTransactionPublisher flaggedPublisher;
    @Mock
    private Acknowledgment ack;

    private TransactionRequest request() {
        return new TransactionRequest(
                "tx-1", "acc_001", new BigDecimal("100"), "USD",
                "merch_001", "name", "Iran", Instant.now());
    }

    private FraudDecision decision(Decision d, int score) {
        return new FraudDecision("tx-1", d, score, List.of(), Instant.now());
    }

    @Test
    void block_decision_is_published_to_flagged_and_acked() {
        when(evaluationService.evaluate(any())).thenReturn(decision(Decision.BLOCK, 90));
        TransactionConsumer consumer = new TransactionConsumer(evaluationService, flaggedPublisher);

        consumer.consume(request(), ack);

        verify(flaggedPublisher).publish(any());
        verify(ack).acknowledge();
    }

    @Test
    void pass_decision_is_acked_but_not_flagged() {
        when(evaluationService.evaluate(any())).thenReturn(decision(Decision.PASS, 0));
        TransactionConsumer consumer = new TransactionConsumer(evaluationService, flaggedPublisher);

        consumer.consume(request(), ack);

        verify(flaggedPublisher, never()).publish(any());
        verify(ack).acknowledge();
    }
}
