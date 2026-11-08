package com.ticketfilms.msadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TopicRequest(
        @NotBlank String nombre,
        @Min(1) int particiones,
        @Min(1) int replicas,
        Map<String, String> configs) {
}
