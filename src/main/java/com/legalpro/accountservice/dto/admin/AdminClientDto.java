package com.legalpro.accountservice.dto.admin;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AdminClientDto {
    private UUID uuid;
    private String name;
    private String email;
    private String phone;
    private int activeCases;
    private String profilePictureUrl;
}
