package com.fidely.domain.dto;

public record CampaignEvent(
        String email,
        String customerName,
        String brandName,
        String message,
        String subject,
        Long walletCardId
) {
}
