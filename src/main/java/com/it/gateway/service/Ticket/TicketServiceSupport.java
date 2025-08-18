package com.it.gateway.service.Ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.enums.Constant;
import com.it.gateway.exception.ApiException;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.utils.KafkaMessageBuilder;

/**
 * Shared helpers for Ticket services to avoid duplicated boilerplate.
 */
final class TicketServiceSupport {

    private TicketServiceSupport() {}

    static <T> void timeout(ConcurrentMap<String, CompletableFuture<List<T>>> pendingRequests,
                            String requestId,
                            Logger log) {
        CompletableFuture<List<T>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(null);
            log.info("Timeout request - requestId: {}", requestId);
        }
    }

    static ApiException apiException(String message, String requestId) {
        return new ApiException(
            ApiException.ErrorCode.INTERNAL_ERROR,
            message,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            requestId
        );
    }

    static <T> CompletableFuture<List<T>> requestList(
            ConcurrentMap<String, CompletableFuture<List<T>>> pendingRequests,
            KafkaTemplate<String, KafkaMessage> kafkaTemplate,
            String requestId,
            String operation,
            Logger log) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        try {
            log.info("Request list '{}' - requestId: {}", operation, requestId);
            pendingRequests.put(requestId, future);

            KafkaMessage kafkaMessage = KafkaMessageBuilder.buildKafkaMessage(
                requestId,
                operation,
                Constant.ResponseStatus.PROCESSING.getValue(),
                null
            );

            kafkaTemplate.send(Constant.TICKET_EVENT_REQUEST, requestId, kafkaMessage);
        } catch (Exception e) {
            log.error("Error requesting list '{}' - requestId: {} | error: {}", operation, requestId, e.getMessage());
            pendingRequests.remove(requestId);
            future.completeExceptionally(apiException(e.getMessage(), requestId));
        }
        return future;
    }

    static <T> void handleListResponse(
            ConcurrentMap<String, CompletableFuture<List<T>>> pendingRequests,
            ObjectMapper objectMapper,
            KafkaMessage message,
            TypeReference<List<T>> typeReference,
            String entityName,
            Logger log) {
        CompletableFuture<List<T>> future = pendingRequests.remove(message.getMessageId());
        if (future == null) {
            log.warn("No pending request found for messageId: {}", message.getMessageId());
            return;
        }

        try {
            if (Constant.ResponseStatus.ERROR.getValue().equals(message.getStatus())) {
                log.error("handle {} response error requestId: {} | Error: {}",
                        capitalize(entityName), message.getMessageId(), message.getErrorMessage());
                future.completeExceptionally(apiException(message.getErrorMessage(), message.getMessageId()));
                return;
            }

            List<T> items = objectMapper.convertValue(message.getPayload(), typeReference);
            log.info("handle {} response success requestId: {} | {}: {}",
                    capitalize(entityName), message.getMessageId(), entityName, items);

            future.complete(items);
        } catch (Exception e) {
            log.error("handle {} response - messageId: {} | error: {}", capitalize(entityName), message.getMessageId(), e.getMessage());
            future.completeExceptionally(apiException(e.getMessage(), message.getMessageId()));
        }
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}


