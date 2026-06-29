package com.fidely.domain.dto.request.stripe;

public record SubscribeRequest(
        String customerId,
        String paymentMethodId
) {
}
