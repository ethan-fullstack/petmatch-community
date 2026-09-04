# 39 — Posibles evoluciones no implementadas

Este capítulo tiene una regla estricta:

> **Nada de lo descrito aquí debe interpretarse como funcionalidad actual de PetMatch Community.**

Hasta el capítulo 38, el libro explica el código que existe en la rama `main`.

Aquí cambiamos deliberadamente de categoría:

```text
estado actual
≠
posible evolución
```

El objetivo es aprender a pensar cómo podría crecer una aplicación Spring Boot sin reescribir la historia y sin fingir que una idea ya fue implementada.

---

# 1. Punto de partida real

PetMatch ya tiene actualmente:

```text
Java 21
Spring Boot 4.1.1
Spring MVC
Thymeleaf
Spring Data JPA / Hibernate
Spring Security
Bean Validation
MySQL
REST JSON
HTTP Basic para /api/**
form login + sesión web
ownership en Services/Repositories
ProblemDetail para errores REST
PESSIMISTIC_WRITE en accept
unit tests con Mockito
integration tests con SpringBootTest / MockMvc
```

También tiene un dominio concreto:

```text
User
Pet
SupportRequest
SupportApplication
```

con flujo principal:

```text
OPEN
→ applications PENDING
→ una ACCEPTED
→ otras REJECTED
→ IN_PROGRESS
→ COMPLETED
```

Cualquier evolución debería partir de ese sistema, no de una aplicación imaginaria distinta.

---

# 2. Qué significa “evolución”

Una evolución puede responder a problemas como:

```text
crecimiento del equipo
crecimiento del tráfico
mayor complejidad del dominio
necesidad de despliegue reproducible
seguridad adicional
mejor estrategia de pruebas
contratos API más maduros
operación y observabilidad
```

Pero una herramienta no debe agregarse solo porque sea popular.

La pregunta correcta es:

> **¿qué problema nuevo justificaría esta complejidad?**

---

# 3. Matriz: actual vs posible

| Tema | Estado actual | Posible evolución |
|---|---|---|
| autenticación web | form login + session | MFA/social login, si existiera necesidad |
| autenticación API | HTTP Basic stateless | OAuth2 Resource Server/JWT, si hubiera clientes desacoplados |
| DB schema | `ddl-auto: update` | migrations con Flyway/Liquibase |
| integración DB de tests | DB configurada externamente | Testcontainers |
| mapping API | manual `ApiDtoMapper` | MapStruct si aumenta mucho el número de DTO |
| locking | pessimistic lock en accept | optimistic locking con `@Version` según patrón de concurrencia |
| documentación API | capítulos Markdown | OpenAPI/Swagger generado |
| despliegue | no documentado como contenedor | Docker/containers si el entorno lo requiere |
| operación | configuración básica | logs estructurados, métricas, tracing |
| UI | Thymeleaf server-side | SPA separada si hay necesidad real |

La columna “posible evolución” **no representa código existente**.

---

# 4. Migraciones de base de datos

## Estado actual

`application.yaml` contiene:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

Esto simplifica el desarrollo educativo del proyecto.

## Problema que podría aparecer

En un equipo o despliegue de producción más maduro necesitamos saber exactamente:

```text
qué cambio de schema ocurrió
cuándo ocurrió
en qué orden
cómo reproducirlo en otro ambiente
```

## Posible evolución

Evaluar herramientas como:

```text
Flyway
Liquibase
```

con scripts/versiones explícitas.

## No implementado

PetMatch **no tiene actualmente** Flyway ni Liquibase configurados.

No debe afirmarse que existe una carpeta de migrations o un historial de schema gestionado por esas herramientas.

---

# 5. Testcontainers

## Estado actual

Las pruebas `@SpringBootTest` dependen del DataSource configurado mediante:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

No se observa H2 ni Testcontainers como infraestructura específica de test.

## Problema futuro posible

Puede ser difícil garantizar que todos ejecuten las integraciones contra una base equivalente y limpia.

## Posible evolución

Usar Testcontainers para levantar temporalmente una instancia de MySQL compatible durante pruebas de integración.

Conceptualmente:

```text
mvn test
→ container MySQL temporal
→ tests
→ destruir container
```

## Beneficio potencial

```text
entorno más reproducible
motor similar al productivo
aislamiento de pruebas
menos configuración manual
```

## Costo

```text
Docker/runtime disponible
más tiempo de arranque
más infraestructura de testing
```

## No implementado

No existen actualmente:

```text
@Testcontainers
@Container
MySQLContainer
```

como parte del proyecto.

---

# 6. Perfil específico de test

## Estado actual

No se observa una configuración:

```text
application-test.yaml
```

que redefina el DataSource para las pruebas.

## Posible evolución

Crear un perfil de testing si aparece la necesidad de separar claramente:

```text
configuración local
configuración de test
configuración de producción
```

Esto podría combinarse con Testcontainers u otro entorno controlado.

## No implementado

PetMatch no tiene hoy una estrategia de profiles documentada como implementación actual.

---

# 7. Optimistic locking con `@Version`

## Estado actual

El punto concurrente más delicado —aceptar una application— usa:

```java
PESSIMISTIC_WRITE
```

sobre la `SupportRequest`.

## Posible problema futuro

En otros patrones de tráfico puede interesar evitar mantener locks pesimistas y detectar conflictos al escribir.

## Posible evolución

Agregar un campo conceptual:

```java
@Version
private Long version;
```

para implementar optimistic locking.

## Diferencia conceptual

```text
pessimistic
→ bloquear antes de competir

optimistic
→ permitir competir y detectar versión obsoleta al actualizar
```

## No implementado

Ninguna Entity actual de PetMatch tiene `@Version`.

El libro no debe describir optimistic locking como parte del flujo presente.

---

# 8. Prueba concurrente real

## Estado actual

Los tests comprueban el algoritmo de `accept` y el flujo integrado, pero son secuenciales.

No lanzan dos transacciones reales al mismo tiempo intentando aceptar applications diferentes de la misma request.

## Posible evolución

Crear un integration test específicamente concurrente para verificar el comportamiento del lock bajo competición real.

Objetivo conceptual:

```text
transaction A → accept application B
transaction B → accept application C

resultado esperado:
solo una ACCEPTED
```

## No implementado

No existe hoy ese test concurrente.

---

# 9. Más cobertura de API REST

## Estado actual probado

`RestApiIntegrationTests` cubre explícitamente:

```text
GET / público web → 200
API anónima → 401
API con Basic → 200
POST Pet → 201 + Location
validation inválida → 400 ProblemDetail
```

## Cobertura futura posible

Agregar tests dedicados para:

```text
404 ownership/visibility
409 business conflict
body JSON ilegible
PUT Pet
DELETE Pet
cancel request
complete request
apply
accept
reject
```

## No implementado

Esos tests dedicados no deben presentarse como parte de la suite actual si no existen en `src/test`.

---

# 10. Pruebas MVC web

## Estado actual

No existe una suite MockMvc completa del flujo HTML:

```text
register
login
create Pet
create request
apply
accept
complete
```

`MvpFlowIntegrationTests` llama Services directamente.

## Posible evolución

Agregar pruebas web que comprueben:

```text
status y redirects
model attributes
validation MVC
CSRF en operaciones POST
visibilidad de páginas
formularios Thymeleaf
```

## No implementado

No debe llamarse al test MVP actual “end-to-end de navegador”.

---

# 11. Pruebas end-to-end con navegador

## Posible necesidad

Si la UI adquiere mayor complejidad, podría ser útil comprobar el comportamiento desde un navegador real.

Herramientas posibles:

```text
Playwright
Selenium
```

Ejemplo de intención:

```text
abrir login
→ ingresar credenciales
→ navegar UI
→ crear Pet
→ verificar HTML final
```

## Costo

Los tests E2E suelen ser:

```text
más lentos
más frágiles
más costosos de mantener
```

## No implementado

PetMatch no usa actualmente Playwright, Selenium ni Cypress.

---

# 12. OpenAPI / Swagger

## Estado actual

La API está documentada pedagógicamente en Markdown y mediante sus Controllers/DTO.

## Posible necesidad

Clientes externos podrían necesitar una especificación navegable/machine-readable de:

```text
routes
request schemas
response schemas
HTTP statuses
```

## Posible evolución

Incorporar una solución OpenAPI si el API pasa a ser un producto consumido por otros equipos.

## No implementado

No debe asumirse que PetMatch tiene Swagger UI u OpenAPI generado actualmente.

---

# 13. API versioning más formal

## Estado actual

Las rutas usan:

```text
/api/v1
```

Eso ya reserva una versión en la URL.

## Posible evolución

Si existieran clientes externos y cambios incompatibles, habría que definir una política explícita:

```text
qué es breaking change
cuánto dura v1
cómo aparece v2
cómo se depreca una versión
```

## No implementado

El repositorio no implementa actualmente una estrategia completa de coexistencia v1/v2.

---

# 14. Resolver la asimetría de `Location` de SupportApplication

## Estado actual

Al crear una application REST se genera un header `Location` hacia:

```text
/api/v1/support-applications/{id}
```

pero no existe actualmente un endpoint individual:

```text
GET /api/v1/support-applications/{id}
```

## Posibles decisiones futuras

Opción A:

```text
crear el GET individual
```

Opción B:

```text
cambiar la Location hacia un recurso realmente navegable
```

Opción C:

```text
revisar si Location es necesaria para ese comando
```

## No implementado

No se agrega ninguno de esos cambios en este libro.

La asimetría se documenta tal como existe.

---

# 15. Mapper automático

## Estado actual

PetMatch usa mapping explícito:

```text
ApiDtoMapper
```

con código Java campo por campo.

## Posible problema futuro

Si el proyecto creciera a decenas o cientos de DTO, el mapping manual podría aumentar significativamente.

## Posible evolución

Evaluar:

```text
MapStruct
```

para generar mapping en compilación.

## ¿Por qué no hacerlo automáticamente ahora?

El mapper manual tiene ventajas pedagógicas:

```text
es visible
es fácil de depurar
enseña las fronteras
no agrega otra herramienta
```

## No implementado

PetMatch no utiliza MapStruct ni ModelMapper actualmente.

---

# 16. Commands / capa de aplicación más neutral

## Estado actual

REST realiza:

```text
API Request DTO
→ ApiDtoMapper
→ Form DTO
→ Service
```

Eso reutiliza los DTO de formulario como entradas del Service.

## Posible problema futuro

Con más interfaces, el nombre `PetForm` podría dejar de representar adecuadamente una entrada de aplicación compartida.

## Posible evolución

Crear objetos neutrales como:

```text
CreatePetCommand
UpdatePetCommand
CreateSupportRequestCommand
```

Entonces:

```text
MVC DTO → command
REST DTO → command
Service → command
```

## No implementado

No existen actualmente esos command objects.

---

# 17. Abstracción de current user

## Estado actual

Los Services reciben:

```java
Authentication authentication
```

y llaman:

```text
UserService.getCurrentUser(authentication)
```

## Posible problema futuro

Si muchos Services dependen directamente del tipo Spring Security `Authentication`, podría crecer el acoplamiento de la capa de aplicación con la infraestructura de seguridad.

## Posible evolución

Crear una abstracción como:

```text
CurrentUser
CurrentUserProvider
AuthenticatedUser
```

que oculte detalles de Spring Security.

## Trade-off

Para el tamaño actual, el diseño existente es directo, legible y fácil de enseñar.

Agregar una abstracción sin necesidad podría dificultar el aprendizaje.

## No implementado

No existe actualmente `CurrentUserProvider` ni equivalente.

---

# 18. Method security

## Estado actual

Ownership se implementa principalmente en Services/Repository queries.

No se observa una estrategia central basada en:

```text
@PreAuthorize
@PostAuthorize
```

para estos casos de ownership.

## Posible evolución

En una aplicación con reglas transversales más complejas podría evaluarse method security.

Ejemplo conceptual:

```java
@PreAuthorize("...")
```

Pero habría que evitar trasladar reglas de dominio difíciles de mantener a expresiones opacas.

## No implementado

PetMatch no usa actualmente `@PreAuthorize` para sus reglas principales.

---

# 19. ACL / motor de políticas

## Estado actual

Las reglas son simples y específicas del dominio:

```text
owner
applicant
outsider
```

## Posible evolución

Si aparecieran organizaciones, equipos, permisos por recurso, delegaciones o políticas dinámicas, podría estudiarse una solución de autorización más sofisticada.

## No implementado

PetMatch no tiene Spring Security ACL ni un policy engine.

No sería apropiado introducirlo mientras ownership sencillo resuelva el problema actual.

---

# 20. OAuth2 / JWT para API

## Estado actual

La API usa:

```text
HTTP Basic
STATELESS
```

con los mismos usuarios de la base.

## Problema futuro que podría justificar cambio

Por ejemplo:

```text
SPA desplegada separadamente
app móvil
integraciones de terceros
authorization server externo
scopes/tokens
SSO
```

## Posible evolución

Evaluar:

```text
OAuth2 Resource Server
JWT access tokens
```

según la arquitectura real.

## Importante

JWT no es automáticamente “más seguro” que una solución correctamente diseñada. Cambia el modelo de credenciales, revocación, expiración, scopes y operación.

## No implementado

PetMatch no tiene actualmente:

```text
JWT service
JWT filter
OAuth2 Resource Server
Authorization Server
```

---

# 21. Login social

## Posible necesidad

Permitir autenticación mediante proveedores externos.

Ejemplos:

```text
Google
GitHub
Microsoft
```

## Posible tecnología

OAuth2 Login / OpenID Connect según proveedor.

## No implementado

El login actual es email/password local.

No existe login social en PetMatch.

---

# 22. Segundo factor de autenticación

## Posible necesidad

Para escenarios con mayor riesgo podría evaluarse MFA/2FA.

## No implementado

PetMatch no tiene actualmente:

```text
TOTP
SMS OTP
WebAuthn
passkeys
2FA
```

No forma parte del alcance educativo presente.

---

# 23. Gestión de secretos más madura

## Estado actual

La base se configura mediante variables de entorno:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Eso evita hardcodear las credenciales directamente en `application.yaml`.

## Posible evolución

En infraestructura productiva podría usarse un gestor de secretos de la plataforma elegida.

## No implementado

PetMatch no integra actualmente un secrets manager concreto.

---

# 24. Docker / contenedores

## Estado actual

Docker no es requisito del proyecto y no debe enseñarse como parte de la implementación actual.

## Posible problema futuro

Necesidad de reproducir empaquetado y runtime en distintos ambientes.

## Posible evolución

Crear un `Dockerfile` y, si corresponde, orquestación local para app + MySQL.

## No implementado

No debe asumirse la existencia actual de:

```text
Dockerfile
docker-compose.yaml
```

como parte del sistema documentado.

---

# 25. CI/CD

## Posible necesidad

Automatizar:

```text
build
tests
quality checks
packaging
deployment
```

al hacer push o pull request.

## Posible evolución

Una pipeline podría ejecutar:

```text
checkout
→ Java setup
→ tests
→ package
→ artifact
```

## No implementado

El libro no documenta actualmente una pipeline CI/CD como funcionalidad del repositorio.

---

# 26. Observabilidad

## Estado actual

No se documenta un stack de observabilidad dedicado.

## Posible evolución

Si la aplicación fuera operada en producción, podría necesitar:

```text
structured logging
metrics
health checks
tracing
alerting
```

Por ejemplo, Spring Boot Actuator podría evaluarse según las necesidades.

## No implementado

No deben inventarse dashboards, Prometheus, Grafana, tracing distribuido o alertas que no existen.

---

# 27. Manejo de errores más rico

## Estado actual

`ApiExceptionHandler` usa:

```text
ProblemDetail
status
title
detail
errors para validation
```

## Posibles extensiones

Si los clientes lo necesitaran, podría considerarse:

```text
errorCode estable
correlationId / traceId
documentation link
lista estructurada de errores
```

## Cuidado

No debe exponerse información interna sensible ni stacktraces al cliente.

## No implementado

El handler actual no contiene un sistema general de `errorCode`, `traceId` o correlation IDs.

---

# 28. Handler genérico de excepciones

## Estado actual

`ApiExceptionHandler` maneja grupos específicos.

No se observó:

```java
@ExceptionHandler(Exception.class)
```

como fallback custom universal.

## Posible evolución

Podría evaluarse un fallback controlado si existe una estrategia clara de logging y respuesta.

## Riesgo

Un handler demasiado amplio puede:

```text
ocultar bugs
convertir todo a status incorrecto
perder información operativa
```

## No implementado

No debe atribuirse al proyecto actual.

---

# 29. Separar mensajes públicos e internos

## Estado actual

El handler ya muestra una señal de esta práctica:

```text
NotFound → usa message de excepción
Conflict → detail genérico
```

## Evolución posible

Formalizar una política consistente para:

```text
mensaje al usuario
mensaje de log
código de error
información sensible
```

Esto sería especialmente útil si la API tuviera consumidores externos.

---

# 30. Pagination

## Estado actual

Los endpoints/listados actuales devuelven listas según el alcance del MVP.

## Posible problema futuro

Con miles de pets, requests o applications, devolver listas completas puede ser ineficiente.

## Posible evolución

Usar Spring Data:

```text
Pageable
Page<T>
Slice<T>
```

junto con un contrato REST de paginación.

## No implementado

No debe describirse la API actual como paginada.

---

# 31. Búsqueda y filtros más ricos

## Estado actual

`SupportRequestRepository` ya tiene algunos métodos por status/type.

## Posible evolución

Si la experiencia lo exigiera:

```text
filtrar por SupportType
rango de fecha
texto
ordenamiento
paginación
```

Podría diseñarse un search endpoint coherente.

## No implementado

Los filtros UI/API que no existen actualmente se consideran una posible evolución.

---

# 32. Cache

## Posible necesidad

Datos muy consultados y poco variables podrían ser candidatos a cache si las mediciones mostraran un problema.

## Posible tecnología

```text
Spring Cache
Redis
```

según arquitectura.

## Riesgo

Cache agrega problemas de:

```text
invalidación
consistencia
memoria
operación
```

## No implementado

PetMatch no usa actualmente Redis ni una estrategia de caching explícita.

---

# 33. Eventos de dominio / notificaciones

## Posible necesidad

Después de:

```text
application accepted
request completed
```

podría surgir la necesidad de enviar notificaciones.

## Posible evolución

Separar la transición de dominio de efectos secundarios como:

```text
email
push notification
in-app notification
```

mediante eventos o servicios específicos.

## No implementado

PetMatch no tiene actualmente un sistema de notificaciones, colas o eventos externos documentado.

---

# 34. Mensajería asíncrona

Si el proyecto creciera y algunos trabajos pudieran ejecutarse fuera de la request HTTP, podrían evaluarse brokers/colas.

Ejemplos tecnológicos posibles:

```text
RabbitMQ
Kafka
```

solo si existe un problema real que los justifique.

## No implementado

No hay actualmente broker, consumer ni producer de mensajería.

---

# 35. Microservicios

## Estado actual

PetMatch es una aplicación Spring Boot monolítica con un dominio pequeño y cohesivo.

## ¿Por qué no dividir automáticamente?

Separar en microservicios agregaría:

```text
red
contratos distribuidos
despliegue múltiple
observabilidad distribuida
consistencia entre servicios
autenticación entre servicios
más operación
```

## Posible evolución

Solo tendría sentido evaluar una separación cuando límites de dominio, escalado/equipos/despliegue lo justificaran.

## No implementado

PetMatch no es una arquitectura de microservicios.

---

# 36. Frontend SPA

## Estado actual

La web usa:

```text
Spring MVC
Thymeleaf
server-side rendering
```

## Posible evolución

Si existiera una necesidad fuerte de UI cliente rica o equipos frontend/backend separados, la REST API podría consumirse desde:

```text
React
Vue
Angular
```

## Implicaciones

Habría que revisar:

```text
autenticación API
CORS
CSRF según arquitectura
tokens/sesiones
deploy separado
contrato REST
```

## No implementado

PetMatch no tiene frontend React, Vue ni Angular.

---

# 37. WebFlux / programación reactiva

## Estado actual

PetMatch usa el stack Servlet/Spring MVC.

## Posible evolución

WebFlux solo debería evaluarse ante requisitos específicos de I/O reactivo/concurrencia y un stack compatible.

## No implementado

PetMatch no usa WebFlux ni Reactor como arquitectura web.

No debe presentarse como una “actualización obligatoria” de MVC.

---

# 38. File & Image Upload — F11

## Estado actual

**NO ESTÁ IMPLEMENTADO.**

Esto debe quedar especialmente claro porque existieron ideas/informes previos que podían sugerir imágenes.

En la rama `main` no se encontró implementación de:

```text
MultipartFile
upload endpoint
storage service
image entity/field de upload
Cloudinary
S3
MinIO
file system upload
```

## Regla del libro

La funcionalidad **F11 File & Image Upload queda excluida como funcionalidad implementada**.

No debe aparecer en capítulos anteriores como algo que PetMatch haga hoy.

---

# 39. Si algún día se implementara upload

Solo como ejercicio conceptual futuro, habría que decidir al menos:

```text
qué archivo se acepta
tamaño máximo
MIME types permitidos
validación real del contenido
nombre generado por servidor
almacenamiento
URL pública o privada
ownership
borrado
protección contra archivos peligrosos
```

También habría que decidir dónde almacenar:

```text
filesystem
object storage
servicio externo
```

## No implementado

Este análisis **no agrega** upload a PetMatch.

No se muestra código ficticio como si perteneciera al repositorio.

---

# 40. S3 / MinIO / Cloudinary

Son alternativas que podrían estudiarse únicamente si existiera una funcionalidad real de archivos/imágenes.

## No implementado

PetMatch no usa actualmente:

```text
AWS S3
MinIO
Cloudinary
```

No debe sugerirse que alguna ya almacena imágenes del proyecto.

---

# 41. Tienda / inventario

## No es parte del dominio actual

PetMatch gestiona apoyo comunitario temporal relacionado con mascotas.

No implementa:

```text
productos
carrito
inventario
órdenes
ventas
pagos
```

Agregar una tienda sería un cambio importante de dominio, no una pequeña extensión técnica.

---

# 42. Pagos

No existe integración de pagos.

Antes de pensar en Stripe, Mercado Pago u otro proveedor habría que definir un caso de negocio que hoy no forma parte del proyecto.

## No implementado

```text
payment
checkout
subscription
invoice
```

no pertenecen al estado actual.

---

# 43. Chat

No existe chat entre owner y applicant.

Una evolución de ese tipo implicaría decisiones sobre:

```text
conversaciones
participantes
mensajes
moderación
notificaciones
privacidad
WebSocket o polling
retención
```

## No implementado

No existen `Chat`, `Conversation` ni `Message` como modelos actuales.

---

# 44. Geolocalización

No existe un módulo de ubicación o mapas.

Una evolución requeriría definir:

```text
qué coordenadas se guardan
precisión
privacidad de ubicación
búsqueda por distancia
proveedor de mapas
```

## No implementado

No debe atribuirse geolocalización a PetMatch.

---

# 45. Red social completa

Aunque PetMatch tiene usuarios y colaboración comunitaria, **no es una red social completa**.

No implementa actualmente:

```text
feed social
followers
likes
comments generales
posts sociales
stories
mensajería social
```

Agregar esas capacidades cambiaría considerablemente el alcance.

---

# 46. Sistema de adopciones

PetMatch no es un sistema de adopción de mascotas.

Su flujo actual se centra en:

```text
apoyo temporal
```

No hay:

```text
adoption application
animal shelter
adoption status
adoption contract
```

No debe confundirse `SupportApplication` con una solicitud de adopción.

---

# 47. Task manager

Los estados y solicitudes del dominio no convierten PetMatch en un gestor genérico de tareas.

No existe:

```text
Task
Project
Kanban
Sprint
Assignee genérico
```

El modelo debe explicarse en términos de mascotas y apoyo comunitario.

---

# 48. Administración

## Estado actual

Existe:

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
```

y el enum `Role` contiene `ADMIN`.

## Pero

Eso **no significa que exista un módulo admin funcional completo**.

No debe inventarse:

```text
AdminController
admin dashboard
user management
moderation panel
```

si no están en la rama actual.

## Posible evolución

Diseñar casos administrativos concretos si aparece una necesidad real de moderación/gestión.

---

# 49. Soft delete

## Estado actual

No se documenta un sistema general de soft delete.

## Posible evolución

Para datos que deban conservar historial, podría evaluarse:

```text
deletedAt
active flag específico
queries que excluyen borrados
```

pero debe decidirse por Entity/caso, no aplicarse mecánicamente.

## No implementado

No describir las Entities actuales como si todas tuvieran soft delete.

---

# 50. Auditoría

## Estado actual

Algunas Entities registran timestamps como `registeredAt`, `createdAt` o `appliedAt`.

Eso no equivale a tener un subsistema completo de auditoría.

## Posible evolución

Si fuera necesario:

```text
createdBy
updatedBy
updatedAt
history table
audit events
```

## No implementado

No existe un audit trail general de cambios.

---

# 51. Historial de transiciones

## Estado actual

Las Entities almacenan el estado actual.

No existe un historial que guarde cada transición:

```text
OPEN → IN_PROGRESS
quién
cuándo
```

## Posible evolución

Un historial sería útil si hubiera requisitos de trazabilidad o disputa.

Podría modelarse como evento/audit record.

## No implementado

No debe afirmarse que PetMatch conserva history de estados.

---

# 52. Cancelar después de IN_PROGRESS

## Estado actual

`cancel(...)` exige:

```text
OPEN
```

No existe un flujo especial para cancelar una request ya `IN_PROGRESS`.

## Posible evolución de dominio

Antes de implementarlo habría que decidir:

```text
qué pasa con ACCEPTED
se notifica al applicant
nuevo estado CANCELLED?
hay motivo de cancelación?
puede reabrirse?
```

## No implementado

Las transiciones deben respetar las reglas que permite el Service actual.

---

# 53. Reabrir una request

No existe una operación:

```text
COMPLETED → OPEN
CANCELLED → OPEN
IN_PROGRESS → OPEN
```

Podría diseñarse en otro dominio, pero la máquina de estados actual no la implementa.

---

# 54. Múltiples accepted applications

## Estado actual

El flujo actual busca garantizar:

```text
máximo una ACCEPTED por SupportRequest
```

## Posible cambio de negocio

Si una request futura necesitara varios helpers, habría que rediseñar:

```text
state machine
count accepted
meaning of IN_PROGRESS
completion
UI
constraints/tests
```

No sería solo “quitar un if”.

## No implementado

El dominio actual no soporta varias accepted como caso válido.

---

# 55. Tipos de soporte dinámicos

## Estado actual

`SupportType` es un enum Java cerrado:

```text
WALK
TEMPORARY_CARE
FEEDING
COMPANIONSHIP
TRANSPORTATION
OTHER
```

## Posible evolución

Si administradores necesitaran crear tipos dinámicamente, podría convertirse en una Entity/catalog table.

## Trade-off

Enum actual:

```text
simple
type-safe
fácil de enseñar
```

Entity dinámica:

```text
más flexible
más CRUD/configuración
```

## No implementado

Los tipos actuales no son administrables desde DB/UI.

---

# 56. Localización temporal / timezone

## Estado actual

`serviceDate` usa:

```text
LocalDateTime
```

que no incluye zona horaria.

## Posible problema futuro

Si usuarios estuvieran en varias zonas geográficas, habría que definir una política temporal explícita.

Posibles tipos:

```text
Instant
OffsetDateTime
ZonedDateTime
```

según contrato.

## No implementado

No debe afirmarse que `serviceDate` actual se guarda automáticamente en UTC o America/Bogota.

---

# 57. Rate limiting

## Posible necesidad

Una API pública podría necesitar límites por cliente/IP/token.

## No implementado

PetMatch no tiene rate limiting actual.

No deben inventarse límites como:

```text
100 requests/minute
```

sin código/configuración que los respalde.

---

# 58. Backups y recuperación

Son preocupaciones operativas importantes para producción, pero no están implementadas/documentadas como parte del código PetMatch.

Una estrategia futura debería definir:

```text
backup schedule
retention
restore tests
RPO/RTO
```

según infraestructura real.

---

# 59. Cómo decidir si una evolución vale la pena

Antes de agregar tecnología, responde:

```text
1. ¿qué problema concreto existe?
2. ¿cómo sabemos que existe?
3. ¿qué parte actual no lo resuelve?
4. ¿cuál es la solución más simple?
5. ¿qué complejidad agrega?
6. ¿cómo se prueba?
7. ¿cómo se opera?
8. ¿qué documentación cambia?
```

Si no puedes responder la primera pregunta, probablemente aún no necesitas la nueva herramienta.

---

# 60. Evolución incremental

Una ruta razonable no consiste en agregar todo de una vez.

Ejemplo hipotético:

```text
problema: integration tests poco reproducibles
↓
Testcontainers
↓
problema: schema debe ser versionado
↓
Flyway
↓
problema: API consumida por equipo externo
↓
OpenAPI
↓
problema: app móvil necesita tokens/scopes
↓
OAuth2/JWT
```

Cada paso se justifica por una necesidad distinta.

---

# 61. Evoluciones que no combinan automáticamente

No existe una regla como:

```text
REST → JWT → microservices → Docker → Kubernetes
```

Una aplicación puede ser perfectamente:

```text
monolito
REST
session o Basic
Docker o no Docker
```

según sus requisitos.

La arquitectura no es una lista de tecnologías de moda.

---

# 62. Deuda técnica vs evolución de producto

## Deuda/infraestructura

Ejemplos posibles:

```text
migrations
reproducibilidad de tests
más cobertura
observabilidad
```

## Evolución de producto

Ejemplos posibles:

```text
notificaciones
chat
geolocalización
administración
```

No son la misma categoría y deberían priorizarse con criterios diferentes.

---

# 63. Qué evoluciones serían especialmente pedagógicas

Para continuar aprendiendo Spring sin cambiar demasiado el dominio, ejercicios futuros útiles podrían ser:

```text
1. añadir test REST de 404
2. añadir test REST de 409
3. añadir profile de test
4. migrar schema controladamente
5. probar concurrencia real de accept
6. documentar API con OpenAPI
```

Estos ejercicios profundizan conceptos existentes antes de transformar PetMatch en otro producto.

> [!NOTE]
> Esta lista es una propuesta educativa, no evidencia de implementación.

---

# 64. Qué evoluciones cambian mucho el alcance

Estas ideas requerirían un rediseño de producto mayor:

```text
marketplace
tienda
pagos
chat completo
red social
adopciones
geolocalización avanzada
microservicios
```

No deberían introducirse accidentalmente mientras el objetivo educativo sea comprender Spring Boot mediante el flujo actual.

---

# 65. Separación final: implementado / no implementado

## Implementado

```text
registro/login local
Pet CRUD controlado por ownership
SupportRequest
SupportApplication
máquinas de estado
accept con lock pesimista
Spring MVC + Thymeleaf
REST API
HTTP Basic stateless
ProblemDetail
Mockito
SpringBootTest
MockMvc
```

## No implementado

```text
F11 File & Image Upload
MultipartFile
S3 / MinIO / Cloudinary
JWT
OAuth2
login social
2FA
microservices
WebFlux
Flyway
Liquibase
Testcontainers
Docker como requisito
CI/CD
OpenAPI/Swagger
rate limiting
observabilidad dedicada
secrets manager
SPA React/Vue/Angular
chat
pagos
tienda/inventario
adopciones
geolocalización
red social completa
```

---

# 66. Regla para futuras actualizaciones del libro

Si una de estas evoluciones se implementa algún día, no basta con borrar la palabra “no implementado”.

El libro debería volver a inspeccionar:

```text
código
configuración
tests
rutas
Entities
migrations
seguridad
README
```

y luego mover la explicación correspondiente al bloque conceptual adecuado.

La documentación debe mantenerse alineada con la implementación:

```text
rama main
```

---

# 67. 🧪 Actividad de arquitectura

Escoge tres evoluciones y completa esta tabla:

| Evolución | Problema que resuelve | Cambio mínimo | Riesgo nuevo | Test necesario |
|---|---|---|---|---|
| Ejemplo: Testcontainers | DB de test reproducible | dependencia + container config | runtime Docker | integration suite |
| ... | ... | ... | ... | ... |

Después responde:

> ¿la implementaría hoy o esperaría a tener evidencia de necesidad?

La respuesta puede ser “esperaría”. Una buena arquitectura también consiste en **no agregar complejidad antes de tiempo**.

---

# 68. 🧪 Comprueba que entendiste

1. ¿PetMatch usa actualmente Flyway?
2. ¿Usa Testcontainers?
3. ¿Usa JWT?
4. ¿Usa OAuth2?
5. ¿Tiene `@Version`?
6. ¿Usa MapStruct?
7. ¿Tiene Swagger UI?
8. ¿Tiene frontend React?
9. ¿Tiene File & Image Upload?
10. ¿Existe `MultipartFile` como funcionalidad actual?
11. ¿Tiene S3/MinIO/Cloudinary?
12. ¿Tiene un módulo admin completo?
13. ¿Tiene chat?
14. ¿Tiene pagos?
15. ¿Tiene geolocalización?
16. ¿Es una arquitectura de microservicios?
17. ¿Los tests actuales prueban concurrencia real simultánea?
18. ¿Existe GET individual de SupportApplication REST?
19. ¿Qué debería ocurrir antes de añadir una tecnología nueva?

### Respuestas esperadas

1. No.
2. No.
3. No.
4. No.
5. No.
6. No.
7. No está presente.
8. No.
9. No.
10. No como implementación del proyecto actual.
11. No.
12. No.
13. No.
14. No.
15. No.
16. No; es monolito Spring Boot.
17. No.
18. No en el Controller actual.
19. Identificar un problema/requisito concreto y evaluar costo, alternativa y pruebas.

---

# 69. ✅ Qué debes recordar

- **Este capítulo describe posibilidades, no código existente.**
- Una nueva herramienta necesita justificar un problema real.
- `ddl-auto: update` podría evolucionar a migrations, pero hoy no hay Flyway/Liquibase.
- Las integraciones podrían usar Testcontainers, pero hoy no lo hacen.
- Accept podría explorarse con optimistic locking, pero hoy usa `PESSIMISTIC_WRITE` y no existe `@Version`.
- Mapping podría automatizarse, pero hoy `ApiDtoMapper` es manual.
- La API podría evolucionar hacia OAuth2/JWT, pero hoy usa HTTP Basic stateless.
- PetMatch podría ganar observabilidad, CI/CD o containers, pero no forman parte del estado actual.
- F11 File & Image Upload está explícitamente **no implementado**.
- No existen actualmente MultipartFile, S3, MinIO ni Cloudinary en el flujo.
- Tampoco existen tienda, pagos, chat, adopciones, geolocalización ni una red social completa.
- El diseño monolítico actual es coherente con el tamaño del proyecto; microservicios no son una evolución automática.
- El libro debe seguir siempre el código de `main`, no propuestas históricas.

---

# Cierre del bloque 07 — Referencia

Con los capítulos 36–39 el bloque de referencia queda completo:

```text
36 Git, GitHub y versionado
→ ¿cómo conservar, revisar y compartir cambios?

37 Glosario
→ ¿qué significa este término?

38 Índice
→ ¿dónde está esta clase/concepto?

39 Evoluciones
→ ¿qué podría hacerse en el futuro sin confundirlo con lo actual?
```

Puedes volver a estos capítulos de forma no lineal según la duda que tengas.

---

[← Capítulo 38 — Índice de clases y conceptos](38-indice-de-clases-y-conceptos.md) · [Índice del bloque](README.md) · [Índice general](../README.md)
