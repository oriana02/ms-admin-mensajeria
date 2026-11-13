package com.ticketfilms.msadmin.controller;

import java.util.Map;
import java.util.Set;

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
@RequestMapping("/api/admin/rabbit")
public class RabbitAdminController {

    private final RabbitAdminService servicio;

    public RabbitAdminController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    // ==========================================
    // Listados
    // ==========================================
    @GetMapping("/colas")
    public ResponseEntity<Set<String>> listarColas() {
        return ResponseEntity.ok(servicio.listarColas());
    }

    @GetMapping("/exchanges")
    public ResponseEntity<Set<String>> listarExchanges() {
        return ResponseEntity.ok(servicio.listarExchanges());
    }

    // ==========================================
    // Colas
    // ==========================================
    @PostMapping("/colas")
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody ColaRequest r) {
        servicio.crearCola(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Cola declarada exitosamente", "nombre", r.nombre()));
    }

    @GetMapping("/colas/{nombre}")
    public ResponseEntity<Map<String, Object>> infoCola(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.infoCola(nombre));
    }

    @DeleteMapping("/colas/{nombre}")
    public ResponseEntity<Map<String, String>> eliminarCola(@PathVariable String nombre) {
        servicio.eliminarCola(nombre);
        return ResponseEntity.ok(Map.of("mensaje", "Cola eliminada", "nombre", nombre));
    }

    // ==========================================
    // Exchanges
    // ==========================================
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest r) {
        servicio.crearExchange(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Exchange declarado exitosamente", "nombre", r.nombre(), "tipo", r.tipo()));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Map<String, String>> eliminarExchange(@PathVariable String nombre) {
        servicio.eliminarExchange(nombre);
        return ResponseEntity.ok(Map.of("mensaje", "Exchange eliminado", "nombre", nombre));
    }

    // ==========================================
    // Bindings
    // ==========================================
    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(@Valid @RequestBody BindingRequest r) {
        servicio.crearBinding(r);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Binding declarado exitosamente"));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, String>> eliminarBinding(@Valid @RequestBody BindingRequest r) {
        servicio.eliminarBinding(r);
        return ResponseEntity.ok(Map.of("mensaje", "Binding eliminado exitosamente"));
    }
}
