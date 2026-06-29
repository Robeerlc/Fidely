package com.fidely.domain.dto.request.card;

import jakarta.validation.constraints.NotBlank;

public record RedeemRequest(
        @NotBlank(message = "El UUID de la tarjeta es obligatorio")
        String secureUuid
) {
}
