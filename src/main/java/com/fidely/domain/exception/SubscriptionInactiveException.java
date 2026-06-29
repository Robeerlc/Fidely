package com.fidely.domain.exception;

public class SubscriptionInactiveException extends RuntimeException {
    public SubscriptionInactiveException(String message) {
        super(message);
    }
}