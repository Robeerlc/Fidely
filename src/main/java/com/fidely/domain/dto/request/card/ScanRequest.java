package com.fidely.domain.dto.request.card;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ScanRequest(
        @NotBlank(message = "El UUID de la tarjeta es obligatorio")
        String secureUuid,

        Integer amount,

        List<Long> serviceIds
) {
}
