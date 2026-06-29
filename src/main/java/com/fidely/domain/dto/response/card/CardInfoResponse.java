package com.fidely.domain.dto.response.card;

public record CardInfoResponse(String customerName, Integer currentStamps, Integer maxStamps, boolean completed) {}
