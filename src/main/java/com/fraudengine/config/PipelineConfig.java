package com.fraudengine.config;

import com.fraudengine.domain.pipeline.RulesEngine;
import com.fraudengine.domain.ports.AccountLimitPort;
import com.fraudengine.domain.ports.LocationRiskPort;
import com.fraudengine.domain.ports.MerchantBlacklistPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain pipeline components as Spring beans, injecting
 * the infrastructure adapters that implement each port.
 */
@Configuration
public class PipelineConfig {

    @Bean
    public RulesEngine rulesEngine(MerchantBlacklistPort blacklistPort,
                                   AccountLimitPort accountLimitPort,
                                   LocationRiskPort locationRiskPort) {
        return new RulesEngine(blacklistPort, accountLimitPort, locationRiskPort);
    }
}
