package com.fidely.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessProfileRequest {
    @NotBlank(message = "El nombre de la marca es obligatorio")
    private String brandName;
    private String themeColor;
    private String logoUrl;
    private String heroImageUrl;

    @NotBlank(message = "La descripción de la recompensa es obligatoria")
    private String rewardDescription;

    private String bookingUrl;
    private String instagramUrl;
}