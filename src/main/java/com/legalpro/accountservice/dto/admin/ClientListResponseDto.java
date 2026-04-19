package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class ClientListResponseDto {
    private List<ClientDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
