package com.kuru.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
	@Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SECURITY: Restrict WebSocket origins to match REST API CORS configuration
        String[] allowedOrigins = getAllowedOrigins();
        
        registry.addEndpoint("/ws-location")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    private String[] getAllowedOrigins() {
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isEmpty()) {
            return allowedOriginsEnv.split(",");
        } else {
            return new String[]{
                "http://localhost:3000",
                "http://localhost:5173"
            };
        }
    }

}
