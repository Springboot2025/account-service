package com.legalpro.accountservice.enums;

public enum SubscriptionInvoiceStatus {
    DUE,      // waiting for payment
    PAID,     // a payment for it was approved
    OVERDUE,  // renewal charge failed, being retried
    VOID      // replaced / no longer payable
}
