package com.fidely.domain.dto.request;

import com.fidely.domain.entity.TargetSegment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CampaignRequest(
        @NotNull(message = "Debes seleccionar un segmento de clientes.")
        TargetSegment segment,

        @NotBlank(message = "El asunto del mensaje es obligatorio.")
        String subject,

        @NotBlank(message = "El contenido del mensaje es obligatorio.")
        String messageBody
) {
}
