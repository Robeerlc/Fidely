package com.fidely.domain.dto.response;

import java.time.LocalDateTime;

public record AtRiskCustomerResponse(
        String name,
        String email,
        LocalDateTime lastVisit
) {
}
