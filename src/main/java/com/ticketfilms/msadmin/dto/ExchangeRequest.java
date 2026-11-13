package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,255}$", message = "solo letras, números, punto, guion y guion bajo (1..255 caracteres)")
        String nombre,
        @NotBlank(message = "el tipo es obligatorio")
        @Pattern(regexp = "(?i)^(topic|direct|fanout)$", message = "debe ser TOPIC, DIRECT o FANOUT")
        String tipo
        ) {

}
