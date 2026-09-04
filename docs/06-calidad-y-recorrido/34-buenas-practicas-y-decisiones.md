# 34 — Buenas prácticas y decisiones de diseño

Este capítulo no intenta declarar que PetMatch sea una arquitectura perfecta.

Su objetivo es más útil:

> **entender por qué las decisiones actuales funcionan para este proyecto, qué problemas resuelven, qué trade-offs introducen y qué alternativas serían razonables en una evolución.**

El análisis se refiere al código actual.

Por tanto distinguiremos siempre:

```text
IMPLEMENTADO
```

de:

```text
ALTERNATIVA POSIBLE
```

---

# 1. Una buena práctica no es una regla absoluta

En software casi siempre existe contexto.

Una decisión puede ser adecuada para:

```text
MVP educativo
pocos casos de uso
equipo pequeño
arquitectura monolítica
```

pero insuficiente para:

```text
alta escala
muchos equipos
integraciones externas complejas
requisitos regulatorios
```

No debemos estudiar PetMatch buscando “la única forma correcta”.

Debemos estudiar:

```text
problema
→ decisión
→ beneficio
→ coste
→ alternativa
```

---

# 2. Decisión: arquitectura por capas

## Implementado

PetMatch separa principalmente:

```text
Controller
Service
Repository
Entity/DTO
```

Los Controllers reciben requests.

Los Services concentran reglas.

Los Repositories acceden a persistencia.

Las Entities representan estado persistente del dominio.

---

# 3. Beneficio de separar Controller y Service

Un Controller puede cambiar de interfaz:

```text
MVC
REST
```

sin duplicar la lógica principal.

Ejemplo:

```text
PetController
→ PetService.create

PetRestController
→ PetService.create
```

Esto es una de las decisiones más importantes de PetMatch.

---

# 4. Trade-off de arquitectura por capas

Separar capas introduce más clases y más saltos para seguir un flujo.

En una aplicación minúscula podría parecer más largo que:

```text
Controller → Repository
```

Pero PetMatch ya tiene reglas suficientes para justificar Service:

```text
ownership
normalización
estados
self-apply
duplicados
aceptación coordinada
visibilidad
```

---

# 5. Alternativa posible

En proyectos más complejos podría añadirse una capa de aplicación más explícita:

```text
UseCase
Command Handler
Application Service
```

PetMatch no implementa esa separación adicional.

Sus `Service` cumplen hoy el papel central de aplicación/negocio.

---

# 6. Decisión: constructor injection

## Implementado

Los Services reciben dependencias en constructor.

Ejemplo conceptual:

```java
public PetService(
    PetRepository petRepository,
    SupportRequestRepository supportRequestRepository,
    UserService userService
) {
    ...
}
```

---

# 7. Beneficios de constructor injection

Hace visibles las dependencias obligatorias.

Permite crear la clase en pruebas unitarias mediante Mockito.

Evita depender de field injection como:

```java
@Autowired
private PetRepository repository;
```

en los Services actuales.

---

# 8. Trade-off

Constructores con muchas dependencias pueden convertirse en una señal de que la clase acumula demasiadas responsabilidades.

En PetMatch los Services actuales tienen un número manejable de colaboradores.

Si crecieran mucho, convendría revisar diseño antes de simplemente seguir agregando parámetros.

---

# 9. Decisión: ownership en Service/Repository

## Implementado

PetMatch no confía en que el frontend o Controller oculten recursos.

Usa consultas como:

```text
findByIdAndOwnerId
findByIdAndSupportRequestOwnerId
```

---

# 10. Beneficio

El id enviado por cliente no es suficiente.

Se combina con:

```text
current user
```

Esto reduce errores tipo:

```text
usuario cambia /42 por /43
→ obtiene recurso ajeno
```

---

# 11. Decisión: resolver current user desde Authentication

## Implementado

`UserService.getCurrentUser(...)` utiliza:

```text
Authentication.getName()
→ email
→ UserRepository
```

Los clientes no deciden un `ownerId` o `applicantId` confiable para operaciones sensibles.

---

# 12. Beneficio

La identidad de seguridad se convierte en referencia principal para ownership.

MVC y REST pueden reutilizar exactamente el mismo Service después de tener un `Authentication` válido.

---

# 13. Trade-off

Los Services quedan acoplados al tipo:

```java
Authentication
```

de Spring Security.

Para este proyecto es simple y directo.

---

# 14. Alternativa posible

Una aplicación con mayor separación podría convertir previamente Spring Security a un objeto propio como:

```text
CurrentUser
AuthenticatedUser
UserContext
```

Y hacer que el Service dependa de esa abstracción.

PetMatch no implementa esta capa adicional.

---

# 15. Decisión: email como username de seguridad

## Implementado

`DatabaseUserDetailsService` construye:

```java
.withUsername(user.getEmail())
```

Y la web configura:

```java
.usernameParameter("email")
```

Esto mantiene una identidad consistente entre login y persistencia.

---

# 16. Beneficio

No hay que mantener:

```text
username separado
+
email separado
```

para el flujo actual.

---

# 17. Trade-off

Si en el futuro se quisiera permitir cambio de email frecuente o usernames públicos independientes, esta decisión tendría implicaciones.

Eso no es un problema actual del MVP.

---

# 18. Decisión: `PasswordEncoderFactories.createDelegatingPasswordEncoder()`

## Implementado

PetMatch registra un `PasswordEncoder` mediante:

```java
PasswordEncoderFactories
    .createDelegatingPasswordEncoder()
```

Y almacena:

```text
passwordHash
```

no el password raw.

---

# 19. Beneficio

La lógica de hashing no se implementa manualmente.

Spring Security puede manejar formatos de password mediante el encoder configurado.

---

# 20. Error que se evita

No aparece algo como:

```java
if (rawPassword.equals(user.getPasswordHash()))
```

ni:

```text
guardar password en texto plano
```

---

# 21. Decisión: dos `SecurityFilterChain`

## Implementado

```text
@Order(1) API
@Order(2) web
```

API:

```text
/api/**
HTTP Basic
STATELESS
CSRF disabled
```

Web:

```text
form login
HTTP session
CSRF activo por configuración estándar
```

---

# 22. Beneficio

Las dos interfaces tienen necesidades distintas sin obligar a usar exactamente el mismo mecanismo HTTP.

El dominio sigue compartido después de autenticación.

---

# 23. Trade-off

Tener varias chains exige comprender bien:

```text
matcher
order
qué chain procesa cada request
```

Una configuración mal ordenada podría aplicar una política inesperada.

Por eso el capítulo 29 insiste en `@Order` y `securityMatcher`.

---

# 24. Decisión: REST con HTTP Basic stateless

## Implementado

La API usa:

```text
HTTP Basic
+
STATELESS
```

---

# 25. Beneficio para el MVP

Es fácil de entender y probar con:

```text
curl
Postman
MockMvc
```

No requiere implementar infraestructura de tokens.

---

# 26. Trade-off

HTTP Basic no es una solución ideal para todos los escenarios de producto modernos.

Exige HTTPS en un entorno real y cada request presenta credenciales según el mecanismo actual.

---

# 27. Alternativas posibles

En otro contexto podrían evaluarse:

```text
OAuth2
Bearer tokens
JWT
session-based API
```

Pero PetMatch no implementa ninguna de esas alternativas actualmente.

No deben enseñarse como parte del código existente.

---

# 28. Decisión: CSRF desactivado solo para `/api/**`

## Implementado

La chain API declara:

```java
.csrf(csrf -> csrf.disable())
```

La web no contiene esa desactivación.

---

# 29. Buena separación

Evita el error de pensar:

```text
“como tengo REST, desactivo CSRF en toda la aplicación”
```

La política se limita al mecanismo API actual.

---

# 30. Decisión: DTO separados de Entity

## Implementado

PetMatch usa:

```text
Form DTO
API Request DTO
API Response DTO
Entity
```

como conceptos diferentes.

---

# 31. Beneficio

El cliente no controla automáticamente campos como:

```text
id
owner
status
passwordHash
createdAt
```

Y los response DTO no exponen el grafo JPA completo.

---

# 32. Trade-off

Hay más clases y mapping.

Ejemplo:

```text
Pet
PetForm
PetApiRequest
PetApiResponse
```

Para un proyecto educativo esa repetición también tiene valor porque hace explícitas las fronteras.

---

# 33. Decisión: API DTO como Java `record`

## Implementado

Los DTO REST se expresan mediante records.

Esto reduce boilerplate para objetos de transferencia simples.

---

# 34. Beneficio

Un record comunica que el objeto es una estructura de datos y no una Entity JPA mutable del dominio.

---

# 35. Trade-off

No todo DTO necesita ser record en todos los sistemas.

Algunas APIs pueden requerir construcción más compleja, defaults o comportamiento especial.

PetMatch usa records donde el contrato es simple.

---

# 36. Decisión: mapping manual

## Implementado

`ApiDtoMapper` transforma explícitamente:

```text
API Request → Form DTO
Entity → API Response
```

---

# 37. Beneficio

Es completamente visible para aprendices.

Se entiende exactamente:

```text
qué campo entra
qué campo sale
qué relación se aplana
```

---

# 38. Trade-off

Si la API creciera mucho, el mapping manual puede volverse repetitivo.

---

# 39. Alternativa posible

Herramientas como:

```text
MapStruct
```

podrían reducir boilerplate en otro diseño.

PetMatch no tiene MapStruct configurado actualmente.

---

# 40. Decisión: Service reutiliza Form DTO internamente

## Implementado

La API hace:

```text
ApiRequest
→ ApiDtoMapper
→ Form DTO
→ Service
```

---

# 41. Beneficio

Permite agregar REST sin duplicar Services ni cambiar todas sus firmas.

---

# 42. Trade-off

Nombres como:

```text
PetForm
```

quedan conceptualmente ligados a la interfaz MVC aunque se reutilicen internamente desde REST.

---

# 43. Alternativa posible

Podrían introducirse objetos neutrales:

```text
CreatePetCommand
UpdateSupportRequestCommand
```

que fueran usados por ambas interfaces.

PetMatch no implementa esa capa actualmente.

---

# 44. Decisión: Bean Validation en frontera de entrada

## Implementado

MVC y REST usan constraints como:

```text
@NotBlank
@NotNull
@Size
@Min
@Future
```

---

# 45. Beneficio

Errores estructurales se detectan antes de ejecutar reglas más profundas.

Ejemplo:

```text
age = -1
```

no necesita convertirse en una regla de `PetService`.

---

# 46. Trade-off

Algunos constraints aparecen tanto en Form DTO como en API Request DTO.

Eso puede parecer duplicación.

Pero las dos interfaces mantienen contratos independientes.

---

# 47. Decisión: reglas de negocio en Service

## Implementado

Ejemplos:

```text
request debe estar OPEN
serviceDate debe ser futura para apply
owner no puede self-apply
no duplicate apply
solo IN_PROGRESS puede complete
```

---

# 48. Beneficio

Las reglas se ejecutan independientemente de si la llamada viene de MVC o REST.

No quedan escondidas en HTML, JavaScript o Controller.

---

# 49. Decisión: estados como enum

## Implementado

PetMatch usa:

```text
SupportRequestStatus
SupportApplicationStatus
SupportType
Role
```

---

# 50. Beneficio

Evita strings arbitrarios como:

```text
"open"
"OPENED"
"progress"
```

dispersos en el código.

Los estados posibles están centralizados.

---

# 51. Trade-off

Un enum Java significa que agregar un nuevo estado requiere cambio de código y despliegue.

Para un dominio controlado como este es razonable.

---

# 52. Decisión: transacciones en Service

## Implementado

Operaciones de escritura importantes usan:

```java
@Transactional
```

Y lecturas usan frecuentemente:

```java
@Transactional(readOnly = true)
```

---

# 53. Beneficio

La frontera transaccional coincide en gran medida con el caso de uso.

Ejemplo `accept`:

```text
lock
→ validar
→ ACCEPTED
→ IN_PROGRESS
→ reject others
```

se coordina como una sola operación lógica.

---

# 54. Decisión: dirty checking

## Implementado

En updates de Entities cargadas, los Services modifican propiedades sin llamar siempre a `save()` nuevamente.

Ejemplo:

```java
request.setStatus(...)
```

Dentro de la transacción Hibernate sincroniza el estado managed.

---

# 55. Beneficio

Reduce código repetitivo y aprovecha el modelo de unidad de trabajo de JPA/Hibernate.

---

# 56. Riesgo conceptual para principiantes

Un aprendiz puede pensar:

```text
“si no veo save(), no se guarda”
```

Por eso es importante comprender:

```text
managed Entity
+
transaction
+
dirty checking
```

---

# 57. Decisión: lock pesimista en accept

## Implementado

`SupportRequestRepository.findByIdForUpdate(...)` usa:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

---

# 58. Problema que intenta resolver

Dos owners requests no existen para una misma request, pero sí pueden existir dos operaciones concurrentes sobre distintas applications de la misma request.

El riesgo es:

```text
A acepta B
Casi al mismo tiempo A acepta C
```

sin coordinación suficiente.

---

# 59. Beneficio

El lock serializa la decisión crítica sobre la request en la base de datos compatible.

Después se vuelven a comprobar estados.

---

# 60. Trade-off

Los locks pesimistas pueden reducir concurrencia y aumentar espera si se usan indiscriminadamente.

Aquí se aplican a un caso crítico y concreto.

---

# 61. Alternativa posible

Otro diseño podría usar optimistic locking con:

```java
@Version
```

y manejar conflictos.

PetMatch no tiene `@Version` implementado actualmente.

---

# 62. Decisión: check adicional de ACCEPTED

## Implementado

Después del lock se consulta:

```text
count accepted
```

antes de aceptar.

---

# 63. Beneficio

La regla:

```text
máximo una accepted
```

queda expresada directamente en el caso de uso.

---

# 64. Decisión: rechazar otras PENDING automáticamente

## Implementado

Aceptar una application implica:

```text
selected → ACCEPTED
others PENDING → REJECTED
request → IN_PROGRESS
```

---

# 65. Beneficio

El estado del dominio queda coherente inmediatamente después de una decisión del owner.

No quedan otras postulaciones PENDING sobre una request que ya tiene persona seleccionada.

---

# 66. Decisión: visibilidad basada en relación

## Implementado

Una SupportRequest no `OPEN` puede seguir visible para:

```text
owner
applicant relacionado
```

pero no para un outsider.

---

# 67. Beneficio

El applicant aceptado o rechazado puede seguir consultando una solicitud en la que participó.

A la vez, una request que ya no está abierta deja de ser información general para cualquiera.

---

# 68. Trade-off

La política de visibilidad está codificada dentro de `SupportRequestService.findVisibleRequest`.

Si aparecieran muchos roles/relaciones adicionales podría ser necesario un modelo de autorización más sofisticado.

---

# 69. Alternativa posible

En sistemas grandes podrían aparecer:

```text
policy objects
ACL
method security más granular
@PreAuthorize con servicios de autorización
```

PetMatch no implementa ACL ni una política central avanzada.

---

# 70. Decisión: `open-in-view: false`

## Implementado

`application.yaml` contiene:

```yaml
spring:
  jpa:
    open-in-view: false
```

---

# 71. Beneficio

Obliga a ser más consciente de qué relaciones necesita cada caso de uso antes de salir de la capa transaccional.

Evita depender de lazy loads inesperados durante renderizado o serialización.

---

# 72. Trade-off

Requiere planear fetches con más cuidado.

Por eso los Repositories usan `@EntityGraph` en consultas relevantes.

---

# 73. Decisión: `@EntityGraph` en consultas de lectura

## Implementado

Ejemplo:

```java
@EntityGraph(attributePaths = {"pet", "owner"})
```

sobre consultas de SupportRequest.

---

# 74. Beneficio

Trae asociaciones necesarias para la respuesta o vista sin depender de OSIV.

---

# 75. Trade-off

Un EntityGraph mal elegido podría cargar más datos de los necesarios.

La estrategia de fetch debe corresponder al caso de uso.

---

# 76. Decisión: Repository derived queries

## Implementado

PetMatch usa métodos como:

```text
findByOwnerIdOrderByCreatedAtDesc
findByStatusAndServiceDateAfterOrderByServiceDateAsc
existsByApplicantIdAndSupportRequestId
```

---

# 77. Beneficio

Spring Data genera consultas a partir de nombres y reduce SQL/JPQL repetitivo para casos simples.

---

# 78. Trade-off

Nombres pueden volverse largos cuando la consulta crece.

Para casos más complejos PetMatch ya usa `@Query`, por ejemplo en `findByIdForUpdate`.

---

# 79. Decisión: `ddl-auto: update`

## Implementado

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

---

# 80. Beneficio actual

Reduce fricción durante desarrollo local/educativo porque Hibernate puede ajustar el esquema.

---

# 81. Trade-off importante

No es un sistema de migraciones versionadas.

No documenta explícitamente cada cambio de esquema ni ofrece el control de herramientas dedicadas.

---

# 82. Alternativa posible

Para un entorno productivo más controlado podrían evaluarse:

```text
Flyway
Liquibase
```

PetMatch no tiene ninguna de ellas implementada actualmente.

---

# 83. Decisión: configuración por variables de entorno

## Implementado

```yaml
url: ${DB_URL}
username: ${DB_USERNAME}
password: ${DB_PASSWORD}
```

---

# 84. Beneficio

Las credenciales no están hardcodeadas directamente en `application.yaml`.

---

# 85. Matiz importante

El repositorio ignora `.env`, pero Spring Boot no carga automáticamente un archivo `.env` genérico solo por existir.

El entorno debe proporcionar esas variables por un mecanismo externo.

---

# 86. Decisión: errores REST centralizados

## Implementado

`ApiExceptionHandler` usa:

```java
@RestControllerAdvice
```

para traducir excepciones seleccionadas a `ProblemDetail`.

---

# 87. Beneficio

Los REST Controllers no repiten:

```text
try/catch
status mapping
error body construction
```

en cada endpoint.

---

# 88. Trade-off

El handler actual agrupa distintas excepciones de negocio bajo un mismo `409` genérico.

Eso simplifica el contrato, pero ofrece menos detalle machine-readable sobre la causa específica.

---

# 89. Alternativa posible

Una API más grande podría añadir:

```text
errorCode
problem type URI
correlation id
catálogo de errores
```

PetMatch no implementa esas extensiones actualmente.

---

# 90. Decisión: not found para recursos fuera de ownership

## Implementado

Muchas consultas owned devuelven una excepción NotFound cuando el recurso no pertenece al current user.

---

# 91. Beneficio

No revela fácilmente la existencia de un recurso que el usuario no debería consultar.

---

# 92. Trade-off

Un consumidor no puede distinguir siempre entre:

```text
id inexistente
```

y:

```text
id existente pero no autorizado
```

Esta es una decisión válida, pero debe ser consciente.

---

# 93. Decisión: pruebas unitarias centradas en Service

## Implementado

Mockito se usa para validar reglas como:

```text
cancel → reject PENDING
accept → ACCEPTED + IN_PROGRESS + reject others
expired request → exception
```

---

# 94. Beneficio

Los tests son rápidos y aíslan la decisión del Service.

---

# 95. Limitación

No validan:

```text
SQL real
JPA mapping real
locks reales
Spring Security filters
HTTP
```

Por eso existen pruebas de integración.

---

# 96. Decisión: pruebas de integración del flujo

## Implementado

`MvpFlowIntegrationTests` ejecuta Services y Repositories reales dentro de `@SpringBootTest`.

---

# 97. Beneficio

Comprueba cooperación entre componentes y consistencia de estados persistidos.

---

# 98. Limitación actual

No simula competencia real entre threads para probar el lock.

Tampoco cubre login HTTP web.

---

# 99. Decisión: MockMvc para REST

## Implementado

`RestApiIntegrationTests` usa:

```text
MockMvc
httpBasic
status
header
jsonPath
```

---

# 100. Beneficio

Prueba el contrato HTTP sin desplegar un servidor externo manualmente.

Integra routing, Security, validation y serialización.

---

# 101. Limitación actual de infraestructura de tests

No hay:

```text
H2 dedicado
Testcontainers
src/test/resources con datasource específico
```

presente en el repositorio.

Las pruebas `@SpringBootTest` pueden depender de las variables de DB del entorno.

---

# 102. Alternativa posible

Podría agregarse un entorno reproducible para tests, por ejemplo con una base efímera controlada.

Pero Testcontainers no está implementado actualmente y no debe aparecer como parte del proyecto real.

---

# 103. Decisión: test de flujo con usuarios diferenciados

## Implementado

El flujo usa:

```text
Owner
Applicant B
Applicant C
Outsider
```

---

# 104. Beneficio

Los tests expresan relaciones de autorización, no solo estados abstractos.

Esto hace el test mucho más representativo del dominio.

---

# 105. Decisión: nombres de test descriptivos

Ejemplo:

```text
acceptMovesRequestToInProgressAndRejectsOtherPendingApplications
```

El nombre describe el comportamiento esperado.

Esto ayuda a usar la suite como documentación ejecutable.

---

# 106. Decisión: aplicación monolítica

## Implementado

PetMatch es una aplicación Spring Boot única con módulos por paquetes.

No hay microservicios.

---

# 107. Beneficio

Para este dominio y objetivo educativo:

```text
menos infraestructura
menos despliegues
transacciones locales simples
navegación de código directa
```

---

# 108. Trade-off

Un monolito grande puede requerir más disciplina modular a medida que crece.

Eso no significa que microservicios sean automáticamente la solución.

---

# 109. Alternativa posible

Antes de microservicios, una evolución razonable podría ser mejorar modularidad interna.

PetMatch no necesita documentarse como arquitectura distribuida.

---

# 110. Decisión: Thymeleaf server-side

## Implementado

La interfaz web se renderiza en servidor mediante Spring MVC + Thymeleaf.

---

# 111. Beneficio

Permite enseñar el stack web de Spring sin añadir un frontend SPA separado.

La autenticación de sesión y CSRF se integran naturalmente con el flujo web.

---

# 112. Trade-off

No ofrece la misma arquitectura que una SPA React/Vue/Angular consumiendo exclusivamente REST.

Eso es una elección de interfaz, no un defecto automático.

---

# 113. Alternativa posible

Un SPA podría consumir `/api/v1`, pero PetMatch no contiene React, Vue ni Angular actualmente.

---

# 114. Decisión: no agregar features fuera del dominio principal

## Implementado

El MVP se centra en:

```text
usuarios
mascotas
solicitudes
postulaciones
estados
```

No hay:

```text
chat
pagos
geolocalización
inventario
tienda
red social completa
upload de imágenes
```

---

# 115. Beneficio pedagógico

Reduce ruido y permite profundizar en conceptos Spring importantes:

```text
DI
JPA
Security
MVC
REST
transactions
locking
testing
```

sin convertir el demo en un producto inmanejable.

---

# 116. Buenas prácticas presentes — resumen

```text
constructor injection
separación Controller/Service/Repository
ownership backend
password hashing
DTO separados
validation en frontera
reglas en Service
estados tipados
transactions en casos de uso
lock pesimista en accept
EntityGraph con open-in-view false
Security chains separadas
REST errors centralizados
unit + integration tests
reutilización de Services entre MVC y REST
```

---

# 117. Trade-offs actuales — resumen

```text
Service depende de Authentication
Form DTO reutilizado desde REST
mapping manual
HTTP Basic como auth API
error 409 genérico
no migraciones versionadas
ddl-auto update
integración depende de DB externa del entorno
cobertura de tests todavía acotada
no test concurrente real
```

Ninguno de estos puntos debe presentarse automáticamente como “error”.

Son decisiones con costes que deben evaluarse según contexto.

---

# 118. Alternativas NO implementadas

Pueden estudiarse como evolución, pero no como estado actual:

```text
application commands neutrales
CurrentUser abstraction
MapStruct
OAuth2/JWT
@Version optimistic locking
Flyway/Liquibase
Testcontainers
códigos de error específicos
policy/ACL layer
SPA frontend
```

---

# 119. Cómo evaluar una decisión técnica

Usa esta secuencia:

```text
1. ¿Qué problema intenta resolver?
2. ¿Qué código actual lo implementa?
3. ¿Qué beneficio aporta?
4. ¿Qué coste introduce?
5. ¿Qué riesgo evita?
6. ¿Qué riesgo conserva?
7. ¿Qué alternativa existe?
8. ¿Esa alternativa está implementada o solo propuesta?
```

Este método evita memorizar listas de “best practices” sin contexto.

---

# 120. Ejemplo completo — ownership

## Problema

El cliente controla ids.

## Riesgo

Acceso/modificación de recursos ajenos.

## Decisión actual

```text
Authentication
→ current User
→ Repository query con owner id
```

## Beneficio

Autorización de recurso en backend.

## Trade-off

Services conocen `Authentication`.

## Alternativa

Abstracción `CurrentUser`.

## Estado

```text
actual → Authentication directo
alternativa → no implementada
```

---

# 121. Ejemplo completo — accept concurrente

## Problema

Dos aceptaciones casi simultáneas.

## Invariante

```text
máximo una ACCEPTED
```

## Decisión actual

```text
@Transactional
+
PESSIMISTIC_WRITE sobre SupportRequest
+
revalidación de status
+
count ACCEPTED
```

## Beneficio

Coordina la decisión crítica.

## Trade-off

Lock de DB puede introducir espera.

## Alternativa

Optimistic locking con `@Version`.

## Estado

`@Version` no está implementado.

---

# 122. Ejemplo completo — API DTO

## Problema

No queremos que JPA Entity sea contrato JSON.

## Decisión

```text
ApiRequest
ApiResponse
ApiDtoMapper
```

## Beneficio

Control explícito de entrada y salida.

## Trade-off

Más clases y mapping.

## Alternativa

Mapper automático o application commands neutrales.

## Estado

Mapping manual implementado.

---

# 123. ⚠️ Errores al hablar de buenas prácticas

## Error 1 — “Siempre hay que usar microservicios”

No. PetMatch no los necesita para su objetivo actual.

## Error 2 — “Siempre hay que usar JWT para REST”

No. Es una alternativa, no una ley.

## Error 3 — “Si usa `ddl-auto:update`, el proyecto está mal”

Es cómodo en desarrollo, aunque insuficiente como estrategia de migraciones controladas de producción.

## Error 4 — “Más capas siempre es mejor”

Capas innecesarias también agregan complejidad.

## Error 5 — “Mockito prueba la base de datos”

No.

## Error 6 — “PESSIMISTIC_WRITE hace imposible cualquier error concurrente”

No debemos hacer afirmaciones absolutas; protege el caso diseñado bajo el comportamiento transaccional/DB correspondiente.

## Error 7 — “DTO reemplaza autorización”

No.

## Error 8 — “CSRF disabled en API significa seguridad desactivada”

No.

## Error 9 — Presentar alternativas como features actuales

Siempre separa actual vs posible evolución.

## Error 10 — Copiar best practices sin explicar el problema que resuelven

La decisión debe entenderse dentro del flujo real.

---

# 124. 🛠 Revisión guiada

## Actividad 1 — Arquitectura

Escoge un endpoint MVC y uno REST que creen Pet.

Demuestra dónde divergen y dónde convergen.

## Actividad 2 — Ownership

Encuentra tres queries que incorporen ids del current user.

Explica qué problema evita cada una.

## Actividad 3 — Transacción

Abre `accept(...)` y enumera todas las modificaciones que deben quedar coordinadas.

## Actividad 4 — Persistencia

Relaciona:

```text
open-in-view false
EntityGraph
dirty checking
```

con el flujo real.

## Actividad 5 — Alternativas

Para cada una marca:

```text
implementada
no implementada
```

- JWT
- MapStruct
- PESSIMISTIC_WRITE
- Flyway
- ProblemDetail
- Testcontainers
- EntityGraph
- Thymeleaf

---

# 125. 🧪 Comprueba que entendiste

1. ¿Por qué PetMatch usa Service entre Controller y Repository?
2. ¿Qué ventaja tiene constructor injection?
3. ¿Dónde se hace ownership?
4. ¿Qué trade-off tiene pasar `Authentication` al Service?
5. ¿Qué mecanismo de password usa?
6. ¿Por qué existen dos SecurityFilterChain?
7. ¿Qué auth usa REST actualmente?
8. ¿JWT está implementado?
9. ¿Por qué DTO y Entity están separados?
10. ¿Qué mapping usa PetMatch?
11. ¿Por qué Form DTO reutilizado desde REST puede ser un trade-off?
12. ¿Dónde viven reglas de estado?
13. ¿Qué ventaja tiene `enum` para estados?
14. ¿Dónde se ubican transacciones principales?
15. ¿Qué es dirty checking en este diseño?
16. ¿Qué lock usa accept?
17. ¿Existe `@Version`?
18. ¿Por qué se rechazan otras PENDING?
19. ¿Qué aporta `open-in-view:false` como disciplina?
20. ¿Qué compensa esa decisión en Repositories?
21. ¿Qué estrategia de schema usa actualmente?
22. ¿Tiene Flyway?
23. ¿Cómo se configuran credenciales DB?
24. ¿Qué centraliza `ApiExceptionHandler`?
25. ¿Qué limitación tienen las pruebas de integración actuales respecto a DB?
26. ¿PetMatch es microservicios?
27. ¿Tiene SPA React/Vue/Angular?
28. ¿Qué regla debes usar para hablar de una alternativa?

### Respuestas esperadas

1. Para centralizar negocio reutilizable y separar HTTP de persistencia.
2. Dependencias explícitas y testabilidad.
3. Services + queries Repository filtradas por current user.
4. Acoplamiento a Spring Security; una abstracción propia sería alternativa.
5. `PasswordEncoder` delegating.
6. Web y API tienen mecanismos HTTP diferentes.
7. HTTP Basic stateless.
8. No.
9. Para desacoplar contrato de persistencia y controlar datos.
10. Manual con `ApiDtoMapper`.
11. El nombre/modelo queda ligado a formulario MVC aunque se use como entrada interna común.
12. Principalmente Services.
13. Estados tipados y conjunto controlado.
14. Services.
15. Persistencia automática de cambios sobre Entities managed dentro de transacción.
16. `PESSIMISTIC_WRITE` sobre SupportRequest.
17. No.
18. Para mantener coherencia cuando una ya fue seleccionada.
19. Obliga a preparar relaciones dentro de la frontera de datos/transacción.
20. `@EntityGraph` en consultas relevantes.
21. Hibernate `ddl-auto:update`.
22. No.
23. Variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
24. Traducción de excepciones API a `ProblemDetail`/status.
25. Dependen del datasource del entorno; no hay una DB de test aislada configurada.
26. No; monolito Spring Boot.
27. No.
28. Marcar claramente que no está implementada si es solo evolución.

---

# 126. ✅ Qué debes recordar

- **Una buena práctica tiene contexto, beneficio y coste.**
- PetMatch separa Controller, Service y Repository con responsabilidades claras.
- Constructor injection hace explícitas las dependencias.
- Ownership se verifica en backend usando current user + queries específicas.
- MVC y REST convergen en los mismos Services.
- Passwords se almacenan mediante `PasswordEncoder`, no en texto plano.
- Web y API usan SecurityFilterChain diferentes porque sus mecanismos HTTP son distintos.
- REST usa HTTP Basic stateless; JWT/OAuth2 no están implementados.
- DTO y Entity están separados para controlar el contrato.
- Mapping REST es manual mediante `ApiDtoMapper`.
- Bean Validation protege estructura de entrada; Service protege negocio.
- Estados se modelan con enums y reglas explícitas.
- Casos de escritura usan transacciones y dirty checking.
- `accept` usa lock pesimista sobre SupportRequest.
- `@Version` no está implementado.
- `open-in-view:false` se combina con fetch explícito/`EntityGraph`.
- `ddl-auto:update` es comodidad de desarrollo, no migraciones versionadas.
- Flyway/Liquibase no están implementados.
- Errores REST seleccionados se centralizan con `ProblemDetail`.
- Las pruebas combinan Mockito e integración, pero la cobertura no es total.
- La suite de integración actual depende del datasource del entorno.
- PetMatch es un monolito Spring Boot con Thymeleaf y REST, no microservicios ni SPA.
- Las alternativas deben enseñarse como evoluciones posibles, nunca como código actual.

---

# 🔗 Continúa con

Ya revisamos el flujo completo y las decisiones de diseño.

El último capítulo de este bloque reúne los problemas que un aprendiz puede cometer al implementar o extender una aplicación con esta arquitectura.

Continúa con:

**[Capítulo 35 — Errores frecuentes →](35-errores-frecuentes.md)**

---

[← Capítulo 33 — Flujo completo PetMatch](33-flujo-completo-petmatch.md) · [Índice del bloque](README.md) · [Índice general](../README.md) · [Siguiente → Capítulo 35](35-errores-frecuentes.md)
