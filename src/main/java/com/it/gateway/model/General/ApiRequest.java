package com.it.gateway.model.General;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sortBy;
    private String sortOrder;
}
