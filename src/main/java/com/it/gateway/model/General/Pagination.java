package com.it.gateway.model.General;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pagination {
    private Integer page;
    private Integer limit;
    private Long total;
    private Integer totalPages;
}
