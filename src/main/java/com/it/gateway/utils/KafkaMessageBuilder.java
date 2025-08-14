package com.it.gateway.utils;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.utils.RequestContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaMessageBuilder {

    public static KafkaMessage buildKafkaMessage(String operationType, String status, Object payload) {
        KafkaMessage kafkaMessage = new KafkaMessage();
        kafkaMessage.setMessageId(RequestContext.getCurrentRequestId());
        kafkaMessage.setOperationType(operationType);
        kafkaMessage.setStatus(status);
        kafkaMessage.setPayload(payload);
        kafkaMessage.setCreatedAt(LocalDateTime.now());

        log.info("KafkaMessageBuilder: {}", kafkaMessage);

        return kafkaMessage;
    }

}
