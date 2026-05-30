package com.fraudengine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket configuration. Clients connect to {@code /ws} and
 * subscribe to {@code /topic/fraud-alerts}.
 *
 * <p>Scaling note: in a multi-instance deployment the in-memory simple broker
 * only reaches clients connected to this JVM. Replace {@code enableSimpleBroker}
 * with an external broker relay (or bridge via Redis pub/sub) so an alert raised
 * on any instance reaches every connected dashboard.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ALERT_TOPIC = "/topic/fraud-alerts";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
