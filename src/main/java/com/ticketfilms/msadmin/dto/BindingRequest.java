package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;

// tipoDestino: QUEUE o EXCHANGE
public record BindingRequest(
        @NotBlank String exchange,
        @NotBlank String destino,
        @NotBlank String tipoDestino,
        String routingKey) {
}
