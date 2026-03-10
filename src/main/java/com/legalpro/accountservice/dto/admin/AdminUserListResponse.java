package com.legalpro.accountservice.dto.admin;

import com.legalpro.accountservice.dto.AdminUserDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserListResponse {

    private List<AdminUserDto> users;

    private int page;
    private int size;

    private long totalElements;
    private int totalPages;
}