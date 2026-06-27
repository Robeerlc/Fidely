package com.fidely.dto.request.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RedeemRequest {
    @NotBlank(message = "El UUID de la tarjeta es obligatorio")
    private String secureUuid;

    @NotNull(message = "El ID del comercio es obligatorio")
    private Long businessId;
}