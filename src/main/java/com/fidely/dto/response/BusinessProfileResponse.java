package com.fidely.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BusinessProfileResponse {
    private Long id;
    private String brandName;
    private String themeColor;
    private String logoUrl;
    private String heroImageUrl;
    private String rewardDescription;
    private String bookingUrl;
    private String instagramUrl;
}