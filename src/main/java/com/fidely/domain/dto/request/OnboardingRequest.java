package com.fidely.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OnboardingRequest(
        @NotNull(message = "El ID del negocio es obligatorio")
        Long businessId,

        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono no es válido")
        String phoneNumber,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email
) {
}
