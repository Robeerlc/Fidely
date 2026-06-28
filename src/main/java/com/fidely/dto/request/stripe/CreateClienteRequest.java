package com.fidely.dto.request.stripe;

public record CreateClienteRequest(
        String email,
        String nombre
) {}
