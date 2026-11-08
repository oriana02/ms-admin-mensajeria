package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfigRequest(@NotBlank String clave, @NotBlank String valor) {
}
