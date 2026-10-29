package com.legalpro.accountservice.enums;

public enum SubscriptionInvoiceType {
    INITIAL,       // first payment for a plan
    RENEWAL,       // charged by the renewal job
    SEAT_ADDITION  // pro-rata charge for seats added mid-cycle (FIRM only)
}
