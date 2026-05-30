package com.fraudengine.infrastructure.websocket;

import com.fraudengine.config.WebSocketConfig;
import com.fraudengine.domain.model.FraudAlert;
import com.fraudengine.domain.ports.AlertPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes fraud alerts to STOMP subscribers of {@link WebSocketConfig#ALERT_TOPIC}.
 */
@Component
public class WebSocketAlertAdapter implements AlertPort {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketAlertAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void push(FraudAlert alert) {
        messagingTemplate.convertAndSend(WebSocketConfig.ALERT_TOPIC, alert);
    }
}
