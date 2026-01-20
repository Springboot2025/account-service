package com.legalpro.accountservice.dto;

public record WinRateCardDto(
        Integer percentage,  // null until outcome is implemented
        Integer growth,      // null for now
        String growthLabel
) {}
