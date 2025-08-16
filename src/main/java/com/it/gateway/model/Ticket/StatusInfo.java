package com.it.gateway.model.Ticket;

import java.util.UUID;

import lombok.Data;

@Data
public class StatusInfo {
    private UUID id;
    private String name;
    private String code;
    private String statusType;
    private String color;
    private Boolean isInitial;
    private Boolean isFinal;
    private Integer sortOrder;
}
