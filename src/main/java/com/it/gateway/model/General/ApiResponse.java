package com.it.gateway.model.General;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.it.gateway.enums.Constant.ResponseStatus;
import com.it.gateway.utils.RequestContext;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private String requestId;
    private String errorCode;
    private String errorMessage;
    private Object errorDetails;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS.getValue())
                .data(data)
                .requestId(RequestContext.getCurrentRequestId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS.getValue())
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }

    public static <T> ApiResponse<T> success(String requestId, T data, String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS.getValue())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, Object errorDetails) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR.getValue())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .errorDetails(errorDetails)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }

    public static <T> ApiResponse<T> error(String requestId, String errorCode, String errorMessage) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR.getValue())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .build();
    }



    public static <T> ApiResponse<T> warning(String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.WARNING.getValue())
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }


    public static <T> ApiResponse<T> pending(String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.PENDING.getValue())
                .message(message)
                .timestamp(LocalDateTime.now())
                .requestId(RequestContext.getCurrentRequestId())
                .build();
    }
    
    
}
