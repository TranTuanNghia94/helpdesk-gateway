package com.it.gateway.controller.Ticket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.it.gateway.model.General.ApiResponse;
import com.it.gateway.model.Ticket.PriorityInfo;
import com.it.gateway.service.Ticket.PriorityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/priorities")
@RequiredArgsConstructor
@Slf4j
public class PriorityController {

    private final PriorityService priorityService;

    @GetMapping
    public ApiResponse<List<PriorityInfo>> getAllPriorities() {
        String requestId = UUID.randomUUID().toString();
        log.info("Get all priorities request initiated: {}", requestId);

        CompletableFuture<List<PriorityInfo>> response = priorityService.getAllPriorities(requestId);

        try {
            List<PriorityInfo> priorities = response.get(60, TimeUnit.SECONDS);
            return ApiResponse.success(requestId, priorities, "Get all priorities success");
        }
        catch (Exception e) {
            return ApiResponse.error(requestId, "500", e.getMessage());
        }
    }
}