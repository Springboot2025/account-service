package com.legalpro.accountservice.dto.admin;

import com.legalpro.accountservice.dto.SupportTicketDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTicketListResponse {

    private List<SupportTicketDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
