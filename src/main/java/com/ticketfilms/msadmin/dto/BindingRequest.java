package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// tipoDestino: QUEUE o EXCHANGE
public record BindingRequest(
        @NotBlank(message = "el exchange es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,255}$", message = "solo letras, números, punto, guion y guion bajo (1..255 caracteres)")
        String exchange,
        @NotBlank(message = "el destino es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,255}$", message = "solo letras, números, punto, guion y guion bajo (1..255 caracteres)")
        String destino,
        @NotBlank(message = "el tipoDestino es obligatorio")
        @Pattern(regexp = "(?i)^(queue|exchange)$", message = "debe ser QUEUE o EXCHANGE")
        String tipoDestino,
        String routingKey
        ) {

}
