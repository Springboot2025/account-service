package com.legalpro.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LawyerSearchRequestDto {
    private String firstName;
    private String lastName;
    private String email;

    private String city;
    private String state;
    private String country;
    private String postalCode;

    private String mobile;
    private String homePhone;

    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
