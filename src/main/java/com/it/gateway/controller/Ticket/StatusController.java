package com.it.gateway.controller.Ticket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.it.gateway.model.General.ApiResponse;
import com.it.gateway.model.Ticket.StatusInfo;
import com.it.gateway.service.Ticket.StatusService;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/status")
@RequiredArgsConstructor
@Slf4j
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    public ApiResponse<List<StatusInfo>> getAllStatuses() {
        String requestId = UUID.randomUUID().toString();
        log.info("Get all statuses request initiated: {}", requestId);

        try {
            List<StatusInfo> statuses = statusService.getAllStatuses(requestId).get(60, TimeUnit.SECONDS);
            return ApiResponse.success(requestId, statuses, "Get all statuses success");
        }
        catch (Exception e) {
            log.error("Get all statuses error: {}", e.getMessage());
            return ApiResponse.error(requestId, "500", e.getMessage());
        }
    }
}
