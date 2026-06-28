package com.fidely.dto.request.card;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanRequest {
    @NotBlank(message = "El UUID de la tarjeta es obligatorio")
    private String secureUuid;
}