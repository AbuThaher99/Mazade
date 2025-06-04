package com.Mazade.project.WebApi.Config;

import com.Mazade.project.Core.Servecies.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthenticationService authenticationService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // In production, restrict this to specific origins
                .withSockJS();

        // Also add a version without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Handle WebSocket connection with authentication
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            // Validate token and get user
                            var user = authenticationService.extractUserFromToken(token);
                            if (user != null) {
                                // Store user info in session attributes
                                accessor.getSessionAttributes().put("userId", user.getId());
                                accessor.getSessionAttributes().put("userEmail", user.getEmail());
                                log.info("✅ WebSocket authenticated user: {} ({})", user.getEmail(), user.getId());
                            } else {
                                log.warn("❌ Invalid token for WebSocket connection");
                                return null; // Reject connection
                            }
                        } catch (Exception e) {
                            log.error("❌ Error authenticating WebSocket connection: {}", e.getMessage());
                            return null; // Reject connection
                        }
                    } else {
                        log.warn("❌ No authentication token provided for WebSocket connection");
                        // For development, you might want to allow unauthenticated connections
                        // In production, uncomment the next line to reject unauthenticated connections
                        // return null;

                        // For now, set a default user ID for testing
                        accessor.getSessionAttributes().put("userId", 1L);
                        log.warn("⚠️ Using default user ID for unauthenticated WebSocket connection");
                    }
                }

                return message;
            }
        });
    }
}