package com.fidely.ui.dto.costumer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterCustumerRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono no es válido")
        String phoneNumber


){}