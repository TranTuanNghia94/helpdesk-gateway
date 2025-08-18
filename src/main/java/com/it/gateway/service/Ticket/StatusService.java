package com.it.gateway.service.Ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.enums.Operation;
import com.it.gateway.model.Kafka.KafkaMessage;
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
        TicketServiceSupport.timeout(pendingRequests, requestId, log);
    }



    public CompletableFuture<List<StatusInfo>> getAllStatuses(String requestId) {
        return TicketServiceSupport.requestList(
            pendingRequests,
            kafkaTemplate,
            requestId,
            Operation.OPERATION_GET_ALL_STATUSES,
            log
        );
    }

    public void handleStatusResponse(KafkaMessage message) {
        TicketServiceSupport.handleListResponse(
            pendingRequests,
            objectMapper,
            message,
            new TypeReference<List<StatusInfo>>() {},
            "statuses",
            log
        );
    }
}
