package com.it.gateway.utils;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import com.it.gateway.model.Kafka.KafkaMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaMessageBuilder {

    public static KafkaMessage buildKafkaMessage(String operationType, String status, Object payload, String messageId) {
        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setMessageId(messageId);
        kafkaMessage.setOperationType(operationType);
        kafkaMessage.setStatus(status);
        kafkaMessage.setPayload(payload);
        kafkaMessage.setCreatedAt(LocalDateTime.now());

        log.info("KafkaMessageBuilder: {}", kafkaMessage);

        return kafkaMessage;
    }

}
