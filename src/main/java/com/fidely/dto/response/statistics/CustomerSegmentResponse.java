package com.fidely.dto.response.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerSegmentResponse {
    private String customerName;
    private String email;
    private String phoneNumber;
    private Long metricValue; // Usaremos esto para el "Nº de visitas" o "Días desde la última visita"
    private String segmentInfo; // Ej: "12 visitas totales" o "Última visita hace 65 días"
}