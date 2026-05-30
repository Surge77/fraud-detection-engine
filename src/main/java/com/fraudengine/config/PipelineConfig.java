package com.fraudengine.config;

import com.fraudengine.domain.pipeline.DecisionEngine;
import com.fraudengine.domain.pipeline.RiskScorer;
import com.fraudengine.domain.pipeline.RulesEngine;
import com.fraudengine.domain.pipeline.VelocityChecker;
import com.fraudengine.domain.ports.AccountLimitPort;
import com.fraudengine.domain.ports.LocationRiskPort;
import com.fraudengine.domain.ports.MerchantBlacklistPort;
import com.fraudengine.domain.ports.VelocityPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain pipeline components as Spring beans, injecting
 * the infrastructure adapters that implement each port and the externalized
 * {@link FraudProperties}.
 */
@Configuration
public class PipelineConfig {

    @Bean
    public RulesEngine rulesEngine(MerchantBlacklistPort blacklistPort,
                                   AccountLimitPort accountLimitPort,
                                   LocationRiskPort locationRiskPort) {
        return new RulesEngine(blacklistPort, accountLimitPort, locationRiskPort);
    }

    @Bean
    public VelocityChecker velocityChecker(VelocityPort velocityPort, FraudProperties properties) {
        return new VelocityChecker(velocityPort,
                properties.velocity().windowSeconds(),
                properties.velocity().maxTransactions());
    }

    @Bean
    public RiskScorer riskScorer(FraudProperties properties) {
        return new RiskScorer(properties.weights());
    }

    @Bean
    public DecisionEngine decisionEngine(FraudProperties properties) {
        return new DecisionEngine(properties.threshold());
    }
}
