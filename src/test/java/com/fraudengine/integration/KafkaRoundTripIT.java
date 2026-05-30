package com.fraudengine.integration;

import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.IncomingTransactionPublisher;
import com.fraudengine.infrastructure.persistence.AuditLogJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Publishes a transaction to {@code transactions.incoming} and asserts the
 * consumer processes it end-to-end, writing an audit row.
 */
class KafkaRoundTripIT extends AbstractIntegrationTest {

    @Autowired
    private IncomingTransactionPublisher publisher;

    @Autowired
    private AuditLogJpaRepository auditRepository;

    @Test
    void published_transaction_is_consumed_and_audited() {
        String txId = UUID.randomUUID().toString();
        TransactionRequest request = new TransactionRequest(
                txId, "acc_002", new BigDecimal("100.00"), "USD",
                "merch_999", "Clean Store", "United States", Instant.now());

        publisher.publish(request);

        await().atMost(Duration.ofSeconds(15)).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() ->
                        assertThat(auditRepository.findByTransactionId(txId)).isPresent());
    }
}
