package com.fidely.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OnboardingResponse {
    private String walletUrl;
    private String message;
}