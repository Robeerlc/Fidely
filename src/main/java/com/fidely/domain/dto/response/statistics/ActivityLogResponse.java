package com.fidely.domain.dto.response.statistics;

import java.time.LocalDateTime;

public record ActivityLogResponse(String customerName, String action, String employeeName, LocalDateTime timestamp) {
}
