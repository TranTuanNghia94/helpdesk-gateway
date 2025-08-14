package com.it.gateway.service.Kafka;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.gateway.enums.Constant;
import com.it.gateway.exception.ApiException;
import com.it.gateway.model.Kafka.KafkaMessage;
import com.it.gateway.model.User.UserInfo;
import com.it.gateway.service.User.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaMessageHandlerUser {
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @KafkaListener(topics = Constant.USER_EVENT_RESPONSE, groupId = Constant.EVENT_GROUP)
    private void listenUserEvent(KafkaMessage message) {
        try {
            log.info("Received user event response: {}", message.getMessageId());

            if (message.getStatus().equals(Constant.ResponseStatus.ERROR.getValue())) {
                log.error("Error received user event response: {}", message.getMessageId());
                throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, message.getPayload().toString(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), message.getMessageId());
            }
            
            UserInfo userInfo = objectMapper.convertValue(message, UserInfo.class);

            userService.handleLoginResponse(message.getMessageId(), userInfo);
        } catch (Exception e) {
            log.error("Error parsing Kafka message: {}", e.getMessage());
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), message.getMessageId());
        }
    }
}
