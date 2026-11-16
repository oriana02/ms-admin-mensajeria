package com.ticketfilms.msadmin.service;

import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private AdminClient admin;

    @InjectMocks
    private KafkaAdminService servicio;

    // ==========================================
    // Tópicos Internos (__)
    // ==========================================
    @Test
    void noSePuedeEliminarUnTopicoInterno() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.eliminar("__consumer_offsets"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(admin);
    }

    @Test
    void noSePuedeCrearUnTopicoConNombreInterno() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.crear("__mio", 3, 3, Map.of()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(admin);
    }

    // ==========================================
    // Tópicos de Negocio Protegidos
    // ==========================================
    @Test
    void noSePuedeEliminarUnTopicoDeNegocio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.eliminar("compra.fallida"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(admin);
    }

    @Test
    void noSePuedeEliminarUnDltDeNegocio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.eliminar("reserva.expirada.DLT"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(admin);
    }

    @Test
    void noSePuedeModificarConfiguracionDeUnTopicoDeNegocio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.modificarConfig("compra.fallida", "retention.ms", "1000"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(admin);
    }

    @Test
    void noSePuedeAmpliarParticionesDeUnTopicoDeNegocio() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.ampliarParticiones("reserva.expirada", 5));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(admin);
    }
}

