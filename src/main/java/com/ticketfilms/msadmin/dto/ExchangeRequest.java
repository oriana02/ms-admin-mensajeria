package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;

// tipo: TOPIC, DIRECT o FANOUT
public record ExchangeRequest(@NotBlank String nombre, @NotBlank String tipo) {
}
