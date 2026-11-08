package com.ticketfilms.msadmin.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ticketfilms.msadmin.dto.BindingRequest;
import com.ticketfilms.msadmin.dto.ColaRequest;
import com.ticketfilms.msadmin.dto.ExchangeRequest;

// Capa de gestión sobre RabbitMQ (AmqpAdmin): crear/eliminar exchanges, colas y bindings.
// Declarar es idempotente si los argumentos coinciden; si difieren, RabbitMQ
// rechaza con PRECONDITION_FAILED (se responde 409 en GlobalExceptionHandler).
@Service
public class RabbitAdminService {

    static final String EX_EVENTOS = "ex.tickets.eventos";
    static final String EX_DLX = "ex.tickets.dlx";
    private static final String SUFIJO_DLQ = ".dlq";
    // Si se borran, todo el flujo de eventos deja de enrutarse
    private static final Set<String> EXCHANGES_BASE = Set.of(EX_EVENTOS, EX_DLX);

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public void crearExchange(ExchangeRequest r) {
        Exchange exchange = switch (r.tipo().toUpperCase()) {
            case "TOPIC" -> new TopicExchange(r.nombre(), true, false);
            case "DIRECT" -> new DirectExchange(r.nombre(), true, false);
            case "FANOUT" -> new FanoutExchange(r.nombre(), true, false);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo inválido: use TOPIC, DIRECT o FANOUT");
        };
        amqpAdmin.declareExchange(exchange);
    }

    public void eliminarExchange(String nombre) {
        if (EXCHANGES_BASE.contains(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "'" + nombre + "' es un exchange base del sistema y no se elimina desde aquí");
        }
        amqpAdmin.deleteExchange(nombre);
    }

    public void crearCola(ColaRequest r) {
        boolean conDlq = r.conDlq() == null || r.conDlq();
        String nombreDlq = r.nombre() + SUFIJO_DLQ;

        if (conDlq) {
            DirectExchange dlx = new DirectExchange(EX_DLX, true, false);
            Queue dlq = QueueBuilder.durable(nombreDlq).build();
            amqpAdmin.declareExchange(dlx);
            amqpAdmin.declareQueue(dlq);
            amqpAdmin.declareBinding(BindingBuilder.bind(dlq).to(dlx).with(nombreDlq));
            amqpAdmin.declareQueue(QueueBuilder.durable(r.nombre())
                    .deadLetterExchange(EX_DLX)
                    .deadLetterRoutingKey(nombreDlq)
                    .build());
        } else {
            amqpAdmin.declareQueue(QueueBuilder.durable(r.nombre()).build());
        }
    }

    public void eliminarCola(String nombre) {
        amqpAdmin.deleteQueue(nombre);
    }

    public Map<String, Object> infoCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La cola no existe");
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("nombre", info.getName());
        resultado.put("mensajes", info.getMessageCount());
        resultado.put("consumidores", info.getConsumerCount());
        return resultado;
    }

    public void crearBinding(BindingRequest r) {
        amqpAdmin.declareBinding(armar(r));
    }

    public void eliminarBinding(BindingRequest r) {
        amqpAdmin.removeBinding(armar(r));
    }

    private Binding armar(BindingRequest r) {
        Binding.DestinationType tipo = switch (r.tipoDestino().toUpperCase()) {
            case "QUEUE" -> Binding.DestinationType.QUEUE;
            case "EXCHANGE" -> Binding.DestinationType.EXCHANGE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tipoDestino inválido: use QUEUE o EXCHANGE");
        };
        return new Binding(r.destino(), tipo, r.exchange(),
                r.routingKey() == null ? "" : r.routingKey(), null);
    }
}
