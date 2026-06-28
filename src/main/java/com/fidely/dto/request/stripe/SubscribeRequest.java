package com.fidely.dto.request.stripe;

public record SubscribeRequest(
        String customerId,
        String paymentMethodId
) {}
