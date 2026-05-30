package com.fraudengine.config;

import com.fraudengine.infrastructure.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Declares Kafka topics and the consumer error-handling policy. Failed records
 * are retried a bounded number of times, then routed to the dead-letter topic
 * so a poison message cannot block its partition indefinitely.
 */
@Configuration
public class KafkaConfig {

    private static final int RETRY_INTERVAL_MS = 1000;
    private static final int MAX_RETRIES = 2;

    @Bean
    public NewTopic incomingTopic() {
        return TopicBuilder.name(KafkaTopics.INCOMING).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic flaggedTopic() {
        return TopicBuilder.name(KafkaTopics.FLAGGED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic incomingDltTopic() {
        return TopicBuilder.name(KafkaTopics.INCOMING_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
    }
}
