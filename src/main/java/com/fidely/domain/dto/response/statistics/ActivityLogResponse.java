package com.fidely.domain.dto.response.statistics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityLogResponse {
    private String customerName;
    private String action;
    private String employeeName;
    private LocalDateTime timestamp;
}