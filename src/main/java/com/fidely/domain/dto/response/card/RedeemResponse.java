package com.fidely.domain.dto.response.card;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedeemResponse {
    private boolean success;
    private String message;
}