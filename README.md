# ms-admin-mensajeria (Servicio de Administración Centralizada)

## Propósito
Este microservicio es el **único punto oficial** para la administración de la infraestructura de mensajería (RabbitMQ y Kafka) en TicketFilms. 

## Decisión de Arquitectura
Anteriormente, `ms-notificaciones` tenía un controlador (`RabbitAdminController`) que administraba RabbitMQ. Para evitar la duplicación de responsabilidades, se refactorizó la arquitectura centralizando la administración en este microservicio.

## Capacidades implementadas
1. **RabbitMQ (`/api/admin/rabbit/**`):**
   - Listado de colas y exchanges vía API de Gestión.
   - Creación de colas con DLQs automáticas (Dead Letter Queues).
   - Validación estricta de nombres (Regex).
   - **Protección de infraestructura:** No permite borrar las colas y exchanges base del sistema (ej. `q.boleto.comprado.notificaciones` o `ex.tickets.eventos`).
2. **Kafka (`/api/admin/kafka/**`):**
   - Creación y listado de tópicos.
   - Ampliación de particiones y modificación de configuraciones.
   - Consulta de detalles (líderes, ISR, réplicas).
   - Diagnóstico operativo: Cálculo del **Lag** por consumer group.
3. **Seguridad:** 
   - Spring Security configurado como Resource Server.
   - Exige un JWT válido de Amazon Cognito con el grupo `admin` (`ROLE_admin`).

## Evidencia de funcionamiento
- Tópicos del flujo `compra.fallida` y `reserva.expirada` (3 particiones, 3 réplicas,
  min.insync.replicas=2), visibles en Kafka UI y con `GET /api/admin/kafka/topics/{nombre}`.
- Creación y eliminación de un tópico de prueba (`demo.admin`) por la API (201 y 204).
- Consulta de lag por grupo: `GET /api/admin/kafka/grupos/ms-asientos-analitica/lag`.
- Listado de colas de Rabbit (200) y rechazo de borrado de cola protegida (409).
