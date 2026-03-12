package com.legalpro.accountservice.dto.admin;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewListResponse {

    private List<AdminReviewDto> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}