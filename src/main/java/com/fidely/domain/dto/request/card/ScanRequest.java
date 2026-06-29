package com.fidely.domain.dto.request.card;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(
        @NotBlank(message = "El UUID de la tarjeta es obligatorio")
        String secureUuid,

        Integer amount
) {}
