package com.fidely.domain.dto.response;

public record VipCustomerResponse(
        String name,
        String email,
        Long visitCount
) {}
