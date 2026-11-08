package com.ticketfilms.msadmin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketfilms.msadmin.dto.ConfigRequest;
import com.ticketfilms.msadmin.dto.ParticionesRequest;
import com.ticketfilms.msadmin.dto.TopicRequest;
import com.ticketfilms.msadmin.service.KafkaAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensajeria/kafka")
public class KafkaAdminController {

    private final KafkaAdminService servicio;

    public KafkaAdminController(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, String>> crear(@Valid @RequestBody TopicRequest r) {
        servicio.crear(r.nombre(), r.particiones(), r.replicas(), r.configs());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Tópico creado", "nombre", r.nombre()));
    }

    @GetMapping("/topics")
    public ResponseEntity<List<String>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @GetMapping("/topics/{nombre}")
    public ResponseEntity<Map<String, Object>> detalle(@PathVariable String nombre) {
        return ResponseEntity.ok(servicio.detalle(nombre));
    }

    @PutMapping("/topics/{nombre}/config")
    public ResponseEntity<Map<String, String>> modificarConfig(@PathVariable String nombre,
            @Valid @RequestBody ConfigRequest r) {
        servicio.modificarConfig(nombre, r.clave(), r.valor());
        return ResponseEntity.ok(Map.of("mensaje", "Configuración actualizada", "nombre", nombre));
    }

    @PutMapping("/topics/{nombre}/particiones")
    public ResponseEntity<Map<String, String>> ampliarParticiones(@PathVariable String nombre,
            @Valid @RequestBody ParticionesRequest r) {
        servicio.ampliarParticiones(nombre, r.total());
        return ResponseEntity.ok(Map.of("mensaje", "Particiones ampliadas", "nombre", nombre));
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        servicio.eliminar(nombre);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupos/{grupo}/lag")
    public ResponseEntity<Map<String, Object>> lag(@PathVariable String grupo) {
        return ResponseEntity.ok(servicio.lag(grupo));
    }
}
