package com.fidely.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignEvent {
    private String email;
    private String customerName;
    private String brandName;
    private String message;
    private String subject;
    private Long walletCardId;
}
