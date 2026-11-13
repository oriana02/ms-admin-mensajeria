package com.ticketfilms.msadmin.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.ticketfilms.msadmin.dto.BindingRequest;
import com.ticketfilms.msadmin.dto.ColaRequest;
import com.ticketfilms.msadmin.dto.ExchangeRequest;

// Capa de gestión sobre RabbitMQ. 
// Incluye validaciones estrictas y protección de la infraestructura base.
@Service
public class RabbitAdminService {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");
    private static final Set<String> TIPOS_EXCHANGE = Set.of("direct", "topic", "fanout");

    static final String EX_EVENTOS = "ex.tickets.eventos";
    static final String EX_DLX = "ex.tickets.dlx";
    private static final String SUFIJO_DLQ = ".dlq";

    // 1. Colas que pertenecen al flujo de negocio de TicketFilms y sus DLQs.
    // Estas NO se pueden borrar desde el panel de administración.
    private static final List<String> COLAS_NEGOCIO = List.of(
            "q.boleto.comprado.notificaciones",
            "q.compra.fallida.notificaciones",
            "q.compra.fallida.asientos",
            "q.reserva.expirada.notificaciones"
    );

    // Generamos el set inmutable que incluye las colas y sus DLQs
    private static final Set<String> COLAS_PROTEGIDAS = Stream.concat(
            COLAS_NEGOCIO.stream(),
            COLAS_NEGOCIO.stream().map(c -> c + SUFIJO_DLQ)
    ).collect(Collectors.toUnmodifiableSet());

    // Exchanges base del sistema
    private static final Set<String> EXCHANGES_PROTEGIDOS = Set.of(EX_EVENTOS, EX_DLX);

    private final AmqpAdmin amqpAdmin;
    private final RestClient rabbitManagementClient; // Para listar colas/exchanges

    // Inyectamos el RestClient que configuraste
    public RabbitAdminService(AmqpAdmin amqpAdmin, RestClient rabbitManagementClient) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitManagementClient = rabbitManagementClient;
    }

    // ============================================================
    // Consultas (Listados)
    // ============================================================
    public Set<String> listarColas() {
        return nombres("/api/queues");
    }

    public Set<String> listarExchanges() {
        return nombres("/api/exchanges");
    }

    // ============================================================
    // Colas
    // ============================================================
    public void crearCola(ColaRequest r) {
        validarNombre(r.nombre(), "cola");
        if (r.nombre().endsWith(SUFIJO_DLQ)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los nombres que terminan en '" + SUFIJO_DLQ + "' están reservados; se crean solos con conDlq=true");
        }

        boolean conDlq = r.conDlq() == null || r.conDlq();
        String nombreDlq = r.nombre() + SUFIJO_DLQ;

        if (conDlq) {
            // Crear DLX si no existe (idempotente)
            amqpAdmin.declareExchange(new DirectExchange(EX_DLX, true, false));

            // Crear DLQ y su binding
            Queue dlq = QueueBuilder.durable(nombreDlq).build();
            amqpAdmin.declareQueue(dlq);
            amqpAdmin.declareBinding(BindingBuilder.bind(dlq).to(new DirectExchange(EX_DLX)).with(nombreDlq));

            // Crear cola principal con dead-letter apuntando a la DLQ
            amqpAdmin.declareQueue(QueueBuilder.durable(r.nombre())
                    .deadLetterExchange(EX_DLX)
                    .deadLetterRoutingKey(nombreDlq)
                    .build());
        } else {
            amqpAdmin.declareQueue(QueueBuilder.durable(r.nombre()).build());
        }
    }

    public void eliminarCola(String nombre) {
        validarNombre(nombre, "cola");
        if (COLAS_PROTEGIDAS.contains(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cola '" + nombre + "' es del sistema y no se puede eliminar");
        }
        amqpAdmin.deleteQueue(nombre);
    }

    public Map<String, Object> infoCola(String nombre) {
        validarNombre(nombre, "cola");
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La cola no existe");
        }
        return Map.of(
                "nombre", info.getName(),
                "mensajes", info.getMessageCount(),
                "consumidores", info.getConsumerCount(),
                "protegida", COLAS_PROTEGIDAS.contains(nombre)
        );
    }

    // ============================================================
    // Exchanges
    // ============================================================
    public void crearExchange(ExchangeRequest r) {
        validarNombre(r.nombre(), "exchange");
        if (!TIPOS_EXCHANGE.contains(r.tipo().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo inválido: use TOPIC, DIRECT o FANOUT");
        }

        Exchange exchange = switch (r.tipo().toLowerCase()) {
            case "direct" ->
                new DirectExchange(r.nombre(), true, false);
            case "topic" ->
                new TopicExchange(r.nombre(), true, false);
            default ->
                new FanoutExchange(r.nombre(), true, false);
        };
        amqpAdmin.declareExchange(exchange);
    }

    public void eliminarExchange(String nombre) {
        validarNombre(nombre, "exchange");
        if (EXCHANGES_PROTEGIDOS.contains(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El exchange '" + nombre + "' es del sistema y no se puede eliminar");
        }
        amqpAdmin.deleteExchange(nombre);
    }

    // ============================================================
    // Bindings
    // ============================================================
    public void crearBinding(BindingRequest r) {
        amqpAdmin.declareBinding(armarBinding(r));
    }

    public void eliminarBinding(BindingRequest r) {
        // Si el destino es una cola protegida, no dejamos borrar sus bindings
        if (r.tipoDestino().equalsIgnoreCase("QUEUE") && COLAS_PROTEGIDAS.contains(r.destino())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pueden eliminar los bindings de la cola protegida '" + r.destino() + "'");
        }
        amqpAdmin.removeBinding(armarBinding(r));
    }

    // ============================================================
    // Internos y Utilidades
    // ============================================================
    private Binding armarBinding(BindingRequest r) {
        Binding.DestinationType tipo = switch (r.tipoDestino().toUpperCase()) {
            case "QUEUE" ->
                Binding.DestinationType.QUEUE;
            case "EXCHANGE" ->
                Binding.DestinationType.EXCHANGE;
            default ->
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "tipoDestino inválido: use QUEUE o EXCHANGE");
        };
        return new Binding(r.destino(), tipo, r.exchange(),
                r.routingKey() == null ? "" : r.routingKey(), null);
    }

    private void validarNombre(String valor, String que) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de " + que + " es obligatorio");
        }
        if (!NOMBRE_VALIDO.matcher(valor).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de " + que
                    + " inválido: solo letras, números, punto, guion y guion bajo (1..255 caracteres)");
        }
        if (valor.startsWith("amq.")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los nombres que empiezan con 'amq.' están reservados por RabbitMQ");
        }
    }

    // Lista nombres usando la API de Gestión de RabbitMQ (puerto 15672)
    private Set<String> nombres(String ruta) {
        try {
            List<Map<String, Object>> items = rabbitManagementClient.get().uri(ruta).retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            if (items == null) {
                return Set.of();
            }
            return items.stream()
                    .filter(i -> "/".equals(i.get("vhost")))
                    .map(i -> String.valueOf(i.get("name")))
                    .filter(n -> !n.isBlank() && !n.startsWith("amq."))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar la API de gestión de RabbitMQ", e);
        }
    }
}
