package com.pulsewatch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SsrfProtectionException extends RuntimeException {
    public SsrfProtectionException(String message) {
        super(message);
    }
}
