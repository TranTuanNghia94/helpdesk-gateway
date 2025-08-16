package com.it.gateway.controller.Ticket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.it.gateway.model.General.ApiResponse;
import com.it.gateway.model.Ticket.CategoryInfo;
import com.it.gateway.service.Ticket.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryInfo>> getAllCategories() {
        String requestId = UUID.randomUUID().toString();
        log.info("Get all categories request initiated: {}", requestId);

        CompletableFuture<List<CategoryInfo>> response = categoryService.getAllCategories(requestId);

        try {
            List<CategoryInfo> categories = response.get(60, TimeUnit.SECONDS);
            return ApiResponse.success(requestId, categories, "Get all categories success");
        } catch (Exception e) {
            return ApiResponse.error(requestId, "500", e.getMessage());
        }
    }
}
