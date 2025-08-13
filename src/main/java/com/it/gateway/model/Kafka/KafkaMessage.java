package com.it.gateway.model.Kafka;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessage {
    private String messageId;
    private String author;
    private String operationType;
    private String status;
    private Object payload;
    private LocalDateTime createdAt;
    private String errorCode;
    private String errorMessage;
    private Object errorDetails;
}
