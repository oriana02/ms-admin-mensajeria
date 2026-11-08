package com.ticketfilms.msadmin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private AdminClient admin;

    @InjectMocks
    private KafkaAdminService servicio;

    @Test
    void noSePuedeEliminarUnTopicoInterno() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.eliminar("__consumer_offsets"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(admin);
    }

    @Test
    void noSePuedeCrearUnTopicoConNombreInterno() {
        assertThrows(ResponseStatusException.class,
                () -> servicio.crear("__mio", 3, 3, Map.of()));
        verifyNoInteractions(admin);
    }
}
