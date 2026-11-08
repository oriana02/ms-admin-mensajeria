package com.ticketfilms.msadmin.dto;

import jakarta.validation.constraints.Min;

public record ParticionesRequest(@Min(1) int total) {
}
