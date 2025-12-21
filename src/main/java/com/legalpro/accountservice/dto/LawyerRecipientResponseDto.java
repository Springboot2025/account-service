package com.legalpro.accountservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LawyerRecipientResponseDto {

    private UUID clientUuid;
    private String contactName;
    private String contactInfo;

    private List<CaseDto> cases;
}
