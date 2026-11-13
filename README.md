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
- Creación del tópico `ticketfilms.boletos` y `ticketfilms.reservas` con 3 particiones y 3 réplicas (HTTP 201).
- Listado de colas de Rabbit (HTTP 200).
- Rechazo de borrado de cola protegida (HTTP 409).
