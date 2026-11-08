package com.ticketfilms.msadmin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Capa de gestión sobre Kafka (AdminClient). Los errores del broker se traducen
// a HTTP: 409 ya existe, 404 no existe, 400 petición inválida, 503 sin respuesta.
@Service
public class KafkaAdminService {

    private static final long ESPERA_SEGUNDOS = 10;

    private final AdminClient admin;

    public KafkaAdminService(AdminClient admin) {
        this.admin = admin;
    }

    public void crear(String nombre, int particiones, int replicas, Map<String, String> configs) {
        rechazarInterno(nombre);
        NewTopic topico = new NewTopic(nombre, particiones, (short) replicas);
        if (configs != null && !configs.isEmpty()) {
            topico.configs(configs);
        }
        esperar(admin.createTopics(List.of(topico)).all());
    }

    public List<String> listar() {
        return esperar(admin.listTopics().names()).stream()
                .filter(n -> !n.startsWith("__"))
                .sorted()
                .toList();
    }

    public Map<String, Object> detalle(String nombre) {
        TopicDescription d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);

        List<Map<String, Object>> particiones = d.partitions().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("particion", p.partition());
            m.put("lider", p.leader() != null ? p.leader().id() : null);
            m.put("replicas", p.replicas().stream().map(Node::id).toList());
            m.put("isr", p.isr().stream().map(Node::id).toList());
            return m;
        }).toList();

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);
        Map<String, String> personalizadas = new LinkedHashMap<>();
        for (ConfigEntry e : config.entries()) {
            if (e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG) {
                personalizadas.put(e.name(), e.value());
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("nombre", d.name());
        resultado.put("numParticiones", d.partitions().size());
        resultado.put("factorReplicacion", d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size());
        resultado.put("particiones", particiones);
        resultado.put("configs", personalizadas);
        return resultado;
    }

    public void modificarConfig(String nombre, String clave, String valor) {
        rechazarInterno(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        AlterConfigOp operacion = new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET);
        Map<ConfigResource, Collection<AlterConfigOp>> cambios = Map.of(recurso, List.of(operacion));
        esperar(admin.incrementalAlterConfigs(cambios).all());
    }

    // Kafka solo permite AUMENTAR particiones; y al aumentarlas cambia a qué
    // partición va cada clave, así que el orden por clave no se conserva.
    public void ampliarParticiones(String nombre, int total) {
        rechazarInterno(nombre);
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
    }

    public void eliminar(String nombre) {
        rechazarInterno(nombre);
        esperar(admin.deleteTopics(List.of(nombre)).all());
    }

    // Lag = último offset del tópico - último offset confirmado por el grupo
    public Map<String, Object> lag(String grupo) {
        Map<TopicPartition, OffsetAndMetadata> confirmados
                = esperar(admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata());
        if (confirmados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El grupo '" + grupo + "' no existe o no ha confirmado offsets");
        }
        Map<TopicPartition, OffsetSpec> pedido = confirmados.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> finales = esperar(admin.listOffsets(pedido).all());

        long lagTotal = 0;
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : confirmados.entrySet()) {
            long actual = e.getValue().offset();
            long fin = finales.get(e.getKey()).offset();
            long lag = Math.max(0, fin - actual);
            lagTotal += lag;
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("topico", e.getKey().topic());
            fila.put("particion", e.getKey().partition());
            fila.put("offsetActual", actual);
            fila.put("offsetFinal", fin);
            fila.put("lag", lag);
            filas.add(fila);
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("grupo", grupo);
        resultado.put("lagTotal", lagTotal);
        resultado.put("particiones", filas);
        return resultado;
    }

    private void rechazarInterno(String nombre) {
        if (nombre != null && nombre.startsWith("__")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los tópicos internos de Kafka (__*) no se administran desde aquí");
        }
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operación interrumpida");
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no respondió a tiempo");
        } catch (ExecutionException e) {
            throw traducir(e.getCause());
        }
    }

    private ResponseStatusException traducir(Throwable causa) {
        if (causa instanceof TopicExistsException) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "El tópico ya existe");
        }
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "El tópico no existe");
        }
        if (causa instanceof InvalidTopicException
                || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidRequestException) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, causa.getMessage());
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Kafka rechazó la operación: " + (causa != null ? causa.getMessage() : "sin detalle"));
    }
}
