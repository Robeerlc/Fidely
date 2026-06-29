package com.fidely.domain.dto.response.card;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScanResponse {
    private boolean success;
    private Integer currentStamps;
    private Integer maxStamps;
    private String message;
}