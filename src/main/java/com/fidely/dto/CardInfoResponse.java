package com.fidely.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardInfoResponse {
    private String customerName;
    private Integer currentStamps;
    private Integer maxStamps;
    private boolean isCompleted;
}