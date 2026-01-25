package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDetailsDto {
    private String formattedAddress;
    private String streetAddress;
    private String unit;
    private String city;
    private String state;
    private String postcode;
    private String country;
}

