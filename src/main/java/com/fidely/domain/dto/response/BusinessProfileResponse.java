package com.fidely.domain.dto.response;

public record BusinessProfileResponse(
        Long id,
        String inviteCode,
        String brandName,
        String themeColor,
        String logoUrl,
        String heroImageUrl,
        String rewardDescription,
        String bookingUrl,
        String instagramUrl
) {
}
