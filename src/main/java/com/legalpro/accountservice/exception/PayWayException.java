package com.legalpro.accountservice.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * A PayWay call failed. The status is what our API returns (422 for errors
 * PayWay reports about the request/card, 502 when PayWay is unreachable or
 * misconfigured, 503 when PayWay isn't configured) and the message is safe
 * to show the user. Handled by GlobalExceptionHandler as a ResponseStatusException.
 */
public class PayWayException extends ResponseStatusException {

    public PayWayException(int status, String message) {
        super(HttpStatusCode.valueOf(status), message);
    }

    public PayWayException(int status, String message, Throwable cause) {
        super(HttpStatusCode.valueOf(status), message, cause);
    }
}
