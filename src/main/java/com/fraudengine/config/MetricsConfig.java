package com.fraudengine.config;

import com.fraudengine.observability.VelocityTracker;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers gauges that read live state. Counters and timers are registered in
 * {@link com.fraudengine.observability.FraudMetrics}.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Gauge activeAccountsGauge(MeterRegistry registry, VelocityTracker velocityTracker) {
        return Gauge.builder("fraud.velocity.active_accounts", velocityTracker,
                        VelocityTracker::activeAccountCount)
                .description("Accounts with an active velocity window")
                .register(registry);
    }
}
