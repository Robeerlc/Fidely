package com.fidely.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BusinessConfigRequest(
        @Min(value = 1, message = "El mínimo de sellos es 1")
        @Max(value = 50, message = "El máximo de sellos es 50")
        Integer defaultMaxStamps,

        @DecimalMin(value = "0.01", message = "El ticket medio debe ser mayor que 0")
        Double averageTicketPrice
) {}
