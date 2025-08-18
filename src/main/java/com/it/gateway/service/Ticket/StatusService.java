package com.it.gateway.service.Ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.enums.Constant;
import com.it.gateway.enums.Operation;
import com.it.gateway.exception.ApiException;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.utils.KafkaMessageBuilder;
import com.it.gateway.model.Ticket.StatusInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusService {
    private final ConcurrentMap<String, CompletableFuture<List<StatusInfo>>> pendingRequests = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void timeoutRequest(String requestId) {
        CompletableFuture<List<StatusInfo>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(null);
            log.info("Timeout request - requestId: {}", requestId);
        }
    }


    private ApiException createApiException(String message, String requestId) {
        return new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, message,
                HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
    }


    public CompletableFuture<List<StatusInfo>> getAllStatuses(String requestId) {
        CompletableFuture<List<StatusInfo>> future = new CompletableFuture<>();
        try {
            log.info("Get all statuses request - requestId: {}", requestId);
            pendingRequests.put(requestId, future);

            KafkaMessage kafkaMessage = KafkaMessageBuilder.buildKafkaMessage(requestId,
                    Operation.OPERATION_GET_ALL_STATUSES, "PROCESSING", null);
                    
            kafkaTemplate.send(Constant.TICKET_EVENT_REQUEST, requestId, kafkaMessage);
        } catch (Exception e) {
            log.error("Error get all statuses - requestId: {} | error: {}", requestId, e.getMessage());
            pendingRequests.remove(requestId);
            future.completeExceptionally(createApiException(e.getMessage(), requestId));
        }
        return future;
    }

    public void handleStatusResponse(KafkaMessage message) {
        CompletableFuture<List<StatusInfo>> future = pendingRequests.remove(message.getMessageId());
        if (future == null) {
            log.warn("No pending request found for messageId: {}", message.getMessageId());
            return;
        }

        try {
            if (Constant.ResponseStatus.ERROR.getValue().equals(message.getStatus())) {
                log.error("handleStatusResponse error requestId: {} | Error: {}", message.getMessageId(), message.getErrorMessage());
                future.completeExceptionally(createApiException(message.getErrorMessage(), message.getMessageId()));
                return;
            }

            List<StatusInfo> statuses = objectMapper.convertValue(message.getPayload(), new TypeReference<List<StatusInfo>>() {});
            log.info("handleStatusResponse success requestId: {} | statuses: {}", message.getMessageId(), statuses);

            future.complete(statuses);
        } catch (Exception e) {
            log.error("handleStatusResponse - messageId: {} | error: {}", message.getMessageId(), e.getMessage());
            future.completeExceptionally(createApiException(e.getMessage(), message.getMessageId()));
        }
    }
}
