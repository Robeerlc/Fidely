package com.fidely.dto.request;

import com.fidely.entity.TargetSegment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampaignRequest {
    @NotNull(message = "Debes seleccionar un segmento de clientes.")
    private TargetSegment segment;

    @NotBlank(message = "El asunto del mensaje es obligatorio.")
    private String subject;

    @NotBlank(message = "El contenido del mensaje es obligatorio.")
    private String messageBody;
}