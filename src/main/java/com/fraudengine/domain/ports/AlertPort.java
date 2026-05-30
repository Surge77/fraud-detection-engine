package com.fraudengine.domain.ports;

import com.fraudengine.domain.model.FraudAlert;

/**
 * Outbound port for pushing real-time fraud alerts to subscribed clients.
 */
public interface AlertPort {

    /**
     * Pushes an alert for a blocked transaction.
     *
     * @param alert the alert payload
     */
    void push(FraudAlert alert);
}
