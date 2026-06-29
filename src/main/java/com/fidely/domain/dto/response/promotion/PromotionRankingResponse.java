package com.fidely.domain.dto.response.promotion;

import java.util.List;

public record PromotionRankingResponse(
        PromotionResponse promotion,
        List<RankingEntryResponse> ranking
) {}
