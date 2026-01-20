package com.legalpro.accountservice.dto;

public record SummaryCardDto(
        int count,
        int growth,        // INT growth percentage
        String growthLabel // "from last month", "this week"
) {}
