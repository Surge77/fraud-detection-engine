package com.fraudengine.observability;

import com.fraudengine.domain.model.Decision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Custom fraud-pipeline metrics: transaction counters by outcome and an
 * end-to-end pipeline latency timer. Exposed via {@code /actuator/prometheus}.
 */
@Component
public class FraudMetrics {

    private final Counter total;
    private final Counter blocked;
    private final Counter passed;
    private final Timer pipelineTimer;

    public FraudMetrics(MeterRegistry registry) {
        this.total = Counter.builder("fraud.transactions.total")
                .description("All transactions processed by the pipeline").register(registry);
        this.blocked = Counter.builder("fraud.transactions.blocked")
                .description("Transactions blocked").register(registry);
        this.passed = Counter.builder("fraud.transactions.passed")
                .description("Transactions passed").register(registry);
        this.pipelineTimer = Timer.builder("fraud.pipeline.duration")
                .description("End-to-end pipeline latency").register(registry);
    }

    /**
     * Times the supplied pipeline execution.
     *
     * @param pipeline the pipeline call to time
     * @param <T>      result type
     * @return the pipeline result
     */
    public <T> T time(Supplier<T> pipeline) {
        return pipelineTimer.record(pipeline);
    }

    /**
     * Records a decision against the outcome counters.
     *
     * @param decision the decision reached
     */
    public void countDecision(Decision decision) {
        total.increment();
        if (decision == Decision.BLOCK) {
            blocked.increment();
        } else {
            passed.increment();
        }
    }
}
