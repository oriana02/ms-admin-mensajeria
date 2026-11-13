package com.ticketfilms.msadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RabbitManagementConfig {

    @Bean
    public RestClient rabbitManagementClient(
            @Value("${spring.rabbitmq.management-url:http://localhost:15672}") String url,
            @Value("${spring.rabbitmq.username:guest}") String usuario,
            @Value("${spring.rabbitmq.password:guest}") String clave) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeaders(h -> h.setBasicAuth(usuario, clave))
                .build();
    }
}
