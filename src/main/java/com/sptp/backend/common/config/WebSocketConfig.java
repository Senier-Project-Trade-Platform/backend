package com.sptp.backend.common.config;

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
        // 전송자가 '/app/**' 경로로 메세지 전송
        registry.setApplicationDestinationPrefixes("/app");

        // '/queue/**' or '/topic/**' 경로로 송신되었을 때, 스프링 내장의 simplebroker가 메세지 처리
        // queue는 1:1, topic은 1:n 관계 시 관습적으로 사용
        registry.enableSimpleBroker("/queue", "/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-connection")
                .setAllowedOrigins("*")
                .withSockJS(); // websocket 미지원 브라우저를 위해 추가
    }
}