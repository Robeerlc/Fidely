package com.fidely.domain.dto.request.stripe;

public record CreateClienteRequest(
        String email,
        String nombre
) {
}
