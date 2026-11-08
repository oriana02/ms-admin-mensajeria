package com.ticketfilms.msadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

// Un solo AdminClient compartido, con los mismos parámetros de conexión que
// spring.kafka.*. No conecta de inmediato: si Kafka está caído al arrancar,
// la app igual levanta y cada operación responde 503.
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
