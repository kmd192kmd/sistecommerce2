package com.example.java.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트(Thymeleaf)가 웹소켓에 접속하는 엔드포인트: /ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") 
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 메시지를 받을 때(구독) 사용하는 prefix
        registry.enableSimpleBroker("/topic");
        
        // 클라이언트가 서버로 메시지를 보낼 때 라우팅할 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}