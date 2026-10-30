package com.legalpro.accountservice.enums;

public enum SubscriptionInvoiceType {
    INITIAL,       // the trial period itself (amount 0) -- or, when a plan has no trial, the first real payment
    RENEWAL,       // charged by the renewal job -- for a trial subscriber, this is their first REAL charge
    SEAT_ADDITION, // pro-rata charge for seats added mid-cycle (FIRM only)
    VERIFICATION   // small refundable charge to verify a card at trial signup -- never a real bill
}
