package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class InvitationListResponse {

    private List<InvitationDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
