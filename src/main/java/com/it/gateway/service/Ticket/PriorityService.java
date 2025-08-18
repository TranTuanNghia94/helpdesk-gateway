package com.it.gateway.service.Ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.model.Ticket.PriorityInfo;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.enums.Operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriorityService {
    private final ConcurrentMap<String, CompletableFuture<List<PriorityInfo>>> pendingRequests = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void timeoutRequest(String requestId) {
        TicketServiceSupport.timeout(pendingRequests, requestId, log);
    }



    public CompletableFuture<List<PriorityInfo>> getAllPriorities(String requestId) {
        return TicketServiceSupport.requestList(
            pendingRequests,
            kafkaTemplate,
            requestId,
            Operation.OPERATION_GET_ALL_PRIORITIES,
            log
        );
    }

    public void handlePriorityResponse(KafkaMessage message) {
        TicketServiceSupport.handleListResponse(
            pendingRequests,
            objectMapper,
            message,
            new TypeReference<List<PriorityInfo>>() {},
            "priorities",
            log
        );
    }

}
