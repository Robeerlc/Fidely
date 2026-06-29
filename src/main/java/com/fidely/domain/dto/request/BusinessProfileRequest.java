package com.fidely.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BusinessProfileRequest(
        @NotBlank(message = "El nombre de la marca es obligatorio")
        String brandName,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un código hexadecimal válido (#RRGGBB)")
        String themeColor,

        String logoUrl,
        String heroImageUrl,

        @NotBlank(message = "La descripción de la recompensa es obligatoria")
        String rewardDescription,

        String bookingUrl,
        String instagramUrl
) {}
