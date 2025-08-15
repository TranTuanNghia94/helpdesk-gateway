package com.it.gateway.enums;

public class Constant {
    
    public enum ResponseStatus {
        SUCCESS("SUCCESS"),
        ERROR("ERROR"),
        WARNING("WARNING"),
        PENDING("PENDING"),
        FAILED("FAILED"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        CANCELLED("CANCELLED"),
        REJECTED("REJECTED"),
        APPROVED("APPROVED");

        private final String value;

        ResponseStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public static final String EVENT_GROUP = "helpdesk-gateway";
    public static final String USER_EVENT_REQUEST = "user-event-request";
    public static final String USER_EVENT_RESPONSE = "user-event-response";
}
