package com.fraudengine.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fraudengine.config.KafkaConfig;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Guards the listener factory for ledger events. Every property asserted here has a
 * silent failure mode — a wrong value produces no error, just behavior that looks
 * fine until you go looking for a trace or a dead-lettered record that never arrived.
 */
class LedgerTransferContainerFactoryTest {

    private ConcurrentKafkaListenerContainerFactory<String, String> factory;

    @BeforeEach
    void setUp() {
        KafkaProperties properties = new KafkaProperties();
        properties.setBootstrapServers(java.util.List.of("localhost:9092"));

        // The recoverer inspects the template on construction, so a stub is required
        // even though nothing is dead-lettered in these assertions.
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        when(template.isTransactional()).thenReturn(false);

        KafkaConfig config = new KafkaConfig();
        factory = config.ledgerTransferListenerContainerFactory(
                properties, config.kafkaErrorHandler(template));
    }

    private Map<String, Object> consumerConfig() {
        return factory.getConsumerFactory().getConfigurationProperties();
    }

    @Test
    @DisplayName("deserializes ledger events as raw strings, not as TransactionRequest")
    void factory_usesStringDeserializers() {
        // The default factory targets TransactionRequest. Inheriting it here would
        // fail on every ledger event, since the ledger owns a different schema.
        assertThat(consumerConfig().get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
                .isEqualTo(StringDeserializer.class);
        assertThat(consumerConfig().get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG))
                .isEqualTo(StringDeserializer.class);
    }

    @Test
    @DisplayName("acknowledges manually so an offset commits only after evaluation")
    void factory_usesManualAck() {
        assertThat(factory.getContainerProperties().getAckMode())
                .isEqualTo(ContainerProperties.AckMode.MANUAL);
    }

    @Test
    @DisplayName("enables observation, without which the ledger trace dies at the broker")
    void factory_enablesObservation() {
        // This factory is hand-built and does NOT inherit
        // spring.kafka.listener.observation-enabled. If this regresses, the consumer
        // stops extracting the traceparent header and the platform silently produces
        // two orphan traces instead of one -- with nothing logged.
        assertThat(factory.getContainerProperties().isObservationEnabled())
                .as("hand-built factory must opt into observation explicitly")
                .isTrue();
    }
}
