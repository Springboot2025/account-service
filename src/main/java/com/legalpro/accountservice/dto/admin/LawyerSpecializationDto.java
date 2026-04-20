package com.legalpro.accountservice.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LawyerSpecializationDto {

    private String specialization;
    private long count;
}
