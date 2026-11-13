package com.ticketfilms.msadmin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.ticketfilms.msadmin.config.SecurityConfig;
import com.ticketfilms.msadmin.service.KafkaAdminService;
import com.ticketfilms.msadmin.service.RabbitAdminService;

// issuer-uri falso: el decoder del JWT es perezoso y no consulta a Cognito al arrancar.
@WebMvcTest(controllers = {KafkaAdminController.class, RabbitAdminController.class},
        properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/issuer-de-prueba")
@Import(SecurityConfig.class)
class MensajeriaSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaAdminService kafka;

    @MockBean
    private RabbitAdminService rabbit;

    private RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"));
    }

    private RequestPostProcessor clienteComun() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_clientes"));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSinGrupoAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics").with(clienteComun()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/rabbit/colas").with(clienteComun())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_listaTopicos() throws Exception {
        when(kafka.listar()).thenReturn(List.of("compra.fallida"));

        mockMvc.perform(get("/api/admin/kafka/topics").with(admin()))
                .andExpect(status().isOk());
    }

    @Test
    void admin_creaTopico_devuelve201() throws Exception {
        mockMvc.perform(post("/api/admin/kafka/topics").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"demo.admin\",\"particiones\":3,\"replicas\":3}"))
                .andExpect(status().isCreated());
    }

    @Test
    void admin_conParticionesInvalidas_devuelve400() throws Exception {
        mockMvc.perform(post("/api/admin/kafka/topics").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"demo.admin\",\"particiones\":0,\"replicas\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_creaColaRabbit_devuelve201() throws Exception {
        mockMvc.perform(post("/api/admin/rabbit/colas").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"q.demo.admin\"}"))
                .andExpect(status().isCreated());
    }
}
