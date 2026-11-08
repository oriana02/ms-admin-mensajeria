package com.ticketfilms.msadmin.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketfilms.msadmin.dto.BindingRequest;
import com.ticketfilms.msadmin.dto.ColaRequest;
import com.ticketfilms.msadmin.dto.ExchangeRequest;
import com.ticketfilms.msadmin.service.RabbitAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensajeria/rabbit")
public class RabbitAdminController {

    private final RabbitAdminService servicio;

    public RabbitAdminController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest r) {
        servicio.crearExchange(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Exchange declarado", "nombre", r.nombre()));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        servicio.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/colas")
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody ColaRequest r) {
        servicio.crearCola(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cola declarada", "nombre", r.nombre()));
    }

    @GetMapping("/colas/{nombre}")
    public ResponseEntity<Map<String, Object>> infoCola(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.infoCola(nombre));
    }

    @DeleteMapping("/colas/{nombre}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String nombre) {
        servicio.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(@Valid @RequestBody BindingRequest r) {
        servicio.crearBinding(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensaje", "Binding declarado"));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest r) {
        servicio.eliminarBinding(r);
        return ResponseEntity.noContent().build();
    }
}
