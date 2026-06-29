package com.fidely.domain.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePromotionRequest(
        @NotBlank(message = "El nombre de la promoción es obligatorio")
        String name,

        @NotBlank(message = "La descripción del beneficio es obligatoria")
        String benefitDescription,

        @NotNull(message = "El número de clientes del ranking es obligatorio")
        @Min(value = 1, message = "El ranking debe incluir al menos 1 cliente")
        @Max(value = 200, message = "El ranking no puede superar los 200 clientes")
        Integer topN,

        boolean permanent,

        LocalDate startDate,

        LocalDate endDate
) {}
