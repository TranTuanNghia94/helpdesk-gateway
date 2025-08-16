package com.it.gateway.model.Ticket;

import java.util.UUID;

import lombok.Data;

@Data
public class PriorityInfo {
    private UUID id;
    private String name;
    private String code;
    private Integer level;
    private String description;
    private String color;
    private Integer responseTimeHours;
    private Integer resolutionTimeHours;
    private Boolean isActive;
}
