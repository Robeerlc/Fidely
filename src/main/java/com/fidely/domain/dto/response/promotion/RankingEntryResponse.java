package com.fidely.domain.dto.response.promotion;

public record RankingEntryResponse(
        int position,
        String customerName,
        String customerEmail,
        Long visitCount
) {}
