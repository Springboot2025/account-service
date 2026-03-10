package com.legalpro.accountservice.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminUserListResponse {

    private List<AdminUserDto> users;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}