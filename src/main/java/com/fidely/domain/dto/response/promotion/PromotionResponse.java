package com.fidely.domain.dto.response.promotion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PromotionResponse(
        Long id,
        String name,
        String benefitDescription,
        Integer topN,
        boolean permanent,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        boolean currentlyRunning,
        LocalDateTime createdAt
) {
}
