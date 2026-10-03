package com.legalpro.accountservice.enums;

public enum PayWayTransactionStatus {
    PENDING,   // row saved, PayWay not called yet / no answer yet
    APPROVED,
    DECLINED,  // declined by the bank
    ERROR      // PayWay rejected the request, was unreachable, or never settled
}
