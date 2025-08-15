package com.it.gateway.service.Kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.it.gateway.enums.Constant;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.service.User.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageHandlerUser {
    private final AuthService authService;

    @KafkaListener(topics = Constant.USER_EVENT_RESPONSE, groupId = Constant.EVENT_GROUP, containerFactory = "kafkaListenerContainerFactory")
    private void listenUserEvent(KafkaMessage message) {
        log.info("Received user event response: {}", message.getMessageId());
        authService.handleLoginResponse(message);
    }
}
