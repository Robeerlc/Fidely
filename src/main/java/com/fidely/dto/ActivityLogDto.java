package com.fidely.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityLogDto {
    private String customerName;
    private String action;
    private LocalDateTime timestamp;
}