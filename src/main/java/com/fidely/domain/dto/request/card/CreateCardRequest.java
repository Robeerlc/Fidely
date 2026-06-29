package com.fidely.domain.dto.request.card;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCardRequest(
        @NotNull(message = "El ID del negocio es obligatorio")
        Long businessId,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        String customerName,

        @NotNull(message = "El email del cliente es obligatorio")
        @Email(message = "El formato del email no es válido")
        String customerEmail,

        @NotBlank(message = "El teléfono del cliente es obligatorio")
        @Pattern(regexp = "^(\\+34|0034|34)?[ -]*([6789])[ -]*([0-9][ -]*){8}$", message = "El formato del teléfono no es válido")
        String customerPhone) {
}