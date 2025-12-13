package com.legalpro.accountservice.enums;

public enum QuoteStatus {
    REQUESTED,   // client has sent request
    BOOKED,       // case created using this quote
    PENDING,     // lawyer reviewing
    SENT,        // lawyer sent quote back
    ACCEPTED,    // client accepted
    REJECTED,    // client/lawyer rejected
    CANCELLED    // either side cancelled
}
