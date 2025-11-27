package ca.sheridancollege.koonerga.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	 @Override
	    public void configureMessageBroker(MessageBrokerRegistry config) {
	        // Use /topic as the destination prefix for broadcasts
	        config.enableSimpleBroker("/topic");
	        config.setApplicationDestinationPrefixes("/app");
	    }

	    @Override
	    public void registerStompEndpoints(StompEndpointRegistry registry) {
	        // Endpoint Angular will connect to
	        registry.addEndpoint("/live-scans")
	                .setAllowedOriginPatterns("*") // Change to Angular URL if needed
	                .withSockJS(); // Enables fallback options
	    }
}
