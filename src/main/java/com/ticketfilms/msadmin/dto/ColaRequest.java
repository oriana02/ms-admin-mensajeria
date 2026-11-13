package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// conDlq: si es null o true, crea también "<nombre>.dlq" y la enlaza al DLX
public record ColaRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,255}$", message = "solo letras, números, punto, guion y guion bajo (1..255 caracteres)")
        String nombre,
        Boolean conDlq
        ) {

}
