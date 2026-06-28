package com.fidely.dto.request.stripe;

public record CancelSubscriptionRequest(
        String subscriptionId
) {
}
