package com.it.gateway.service.Ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.exception.ApiException;
import com.it.gateway.model.Ticket.CategoryInfo;
import com.it.gateway.utils.KafkaMessageBuilder;
import com.it.gateway.enums.Constant;
import com.it.gateway.enums.Operation;
import com.it.gateway.model.Kafka.KafkaMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {
    private final ConcurrentMap<String, CompletableFuture<List<CategoryInfo>>> pendingRequests = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void timeoutRequest(String requestId) {
        CompletableFuture<List<CategoryInfo>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(null);
            log.info("Timeout request - requestId: {}", requestId);
        }
    }

    private ApiException createApiException(String message, String requestId) {
        return new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, message,
                HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
    }

    public CompletableFuture<List<CategoryInfo>> getAllCategories(String requestId) {
        CompletableFuture<List<CategoryInfo>> future = new CompletableFuture<>();
        try {
            log.info("Get all categories request - requestId: {}", requestId);
            pendingRequests.put(requestId, future);

            KafkaMessage kafkaMessage = KafkaMessageBuilder.buildKafkaMessage(requestId,
                    Operation.OPERATION_GET_ALL_CATEGORIES, "PROCESSING", null);
            kafkaTemplate.send(Constant.TICKET_EVENT_REQUEST, requestId, kafkaMessage);
        } catch (Exception e) {
            log.error("Error get all categories - requestId: {} | error: {}", requestId, e.getMessage());
            pendingRequests.remove(requestId);
            future.completeExceptionally(createApiException(e.getMessage(), requestId));
        }
        return future;
    }

    public void handleCategoryResponse(KafkaMessage message) {
        CompletableFuture<List<CategoryInfo>> future = pendingRequests.remove(message.getMessageId());
        if (future == null) {
            log.warn("No pending request found for messageId: {}", message.getMessageId());
            return;
        }

        try {
            if (Constant.ResponseStatus.ERROR.getValue().equals(message.getStatus())) {
                log.error("handleCategoryResponse error requestId: {} | Error: {}", message.getMessageId(), message.getErrorMessage());
                future.completeExceptionally(createApiException(message.getErrorMessage(), message.getMessageId()));
                return;
            }

            List<CategoryInfo> categories = objectMapper.convertValue(message.getPayload(), List.class);
            log.info("handleCategoryResponse success requestId: {} | categories: {}", message.getMessageId(),categories);

            future.complete(categories);
        } catch (Exception e) {
            log.error("handleCategoryResponse - messageId: {} | error: {}", message.getMessageId(),
                    e.getMessage());
            future.completeExceptionally(createApiException(e.getMessage(), message.getMessageId()));
        }
    }

}
