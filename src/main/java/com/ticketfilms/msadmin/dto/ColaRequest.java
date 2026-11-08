package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.NotBlank;

// conDlq: si es null o true, crea también "<nombre>.dlq" y la enlaza al DLX
public record ColaRequest(@NotBlank String nombre, Boolean conDlq) {
}
