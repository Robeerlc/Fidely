package com.fidely.domain.dto.response.card;

public record ScanResponse(boolean success, Integer currentStamps, Integer maxStamps, String message) {}
