package com.ticketfilms.msadmin.exception;

import java.util.Map;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Map<String, String>> rabbitSinConexion(AmqpConnectException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No hay conexión con RabbitMQ"));
    }

    // Ej. PRECONDITION_FAILED: la cola o exchange ya existe con otros argumentos
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, String>> rabbitRechazo(AmqpException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "RabbitMQ rechazó la operación: " + ex.getMessage()));
    }
}
