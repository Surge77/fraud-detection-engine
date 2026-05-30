package com.fraudengine.domain.ports;

import com.fraudengine.domain.model.FraudDecision;

/**
 * Outbound port for emitting a blocked transaction's decision to downstream
 * consumers (alerting, case management).
 */
public interface FlaggedTransactionPublisher {

    /**
     * Publishes a blocked decision.
     *
     * @param decision the BLOCK decision to emit
     */
    void publish(FraudDecision decision);
}
