package com.fidely.domain.dto.request.stripe;

public record CancelSubscriptionRequest(
        String subscriptionId
) {
}
