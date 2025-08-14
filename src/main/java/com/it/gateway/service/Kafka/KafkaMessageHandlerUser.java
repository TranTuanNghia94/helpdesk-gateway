package com.it.gateway.service.Kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.model.Kafka.KafkaMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageHandlerUser {

    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(KafkaMessage message, String topic) {
        log.info("Sending message to topic: {} | Message ID: {} | Status: {}", topic, message.getMessageId(), message.getStatus());
        kafkaTemplate.send(topic, message.getMessageId(), message);
    }


    private void listenUserEvent(String message) {
        try {
            KafkaMessage kafkaMessage = objectMapper.readValue(message, KafkaMessage.class);
        } catch (Exception e) {
            log.error("Error parsing Kafka message: {}", e.getMessage());
        }
    }
}
