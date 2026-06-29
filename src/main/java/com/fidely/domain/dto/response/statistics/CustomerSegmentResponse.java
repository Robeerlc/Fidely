package com.fidely.domain.dto.response.statistics;

public record CustomerSegmentResponse(
        String customerName,
        String email,
        String phoneNumber,
        Long metricValue,
        String segmentInfo
) {}
