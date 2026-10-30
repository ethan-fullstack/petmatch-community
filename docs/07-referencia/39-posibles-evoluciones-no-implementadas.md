# 39 — Posibles evoluciones no implementadas

Este capítulo estudia tecnologías y decisiones que **podrían** tener sentido si PetMatch adquiriera nuevas necesidades. No forman parte del comportamiento que has recorrido en los capítulos anteriores.

La distinción central es:

```text
lo que PetMatch hace
≠
lo que Spring o su ecosistema permitirían agregar
```

El objetivo es aprender a evaluar una evolución técnica sin convertir una lista de herramientas populares en una arquitectura.

---

# 1. Punto de partida

PetMatch utiliza:

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
Mockito
SpringBootTest / MockMvc
```

Su dominio central es:

```text
User
Pet
SupportRequest
SupportApplication
```

Cualquier evolución debería responder primero a una necesidad de ese sistema.

---

# 2. Cómo evaluar una evolución

Antes de incorporar una tecnología nueva, pregunta:

```text
1. ¿qué problema concreto existe?
2. ¿qué evidencia tenemos de ese problema?
3. ¿qué parte de la solución actual ya no es suficiente?
4. ¿cuál es el cambio más simple que lo resuelve?
5. ¿qué complejidad agrega?
6. ¿cómo se prueba?
7. ¿cómo se opera?
```

Una herramienta no se justifica únicamente porque sea popular.

---

# 3. Matriz de posibles evoluciones

| Tema en PetMatch | Solución utilizada | Alternativa que podría evaluarse |
|---|---|---|
| esquema de DB | `ddl-auto: update` | Flyway/Liquibase |
| integración DB de tests | datasource configurado externamente | Testcontainers |
| concurrencia de `accept` | `PESSIMISTIC_WRITE` | optimistic locking con `@Version` |
| mapping API | `ApiDtoMapper` manual | MapStruct |
| documentación API | Markdown + código | OpenAPI/Swagger |
| autenticación API | HTTP Basic stateless | OAuth2 Resource Server/JWT |
| interfaz web | Thymeleaf server-side | SPA separada |
| despliegue | ejecución Java/Maven | contenedores si el entorno lo necesita |
| operación | configuración básica | métricas, tracing y logs estructurados |

La columna de alternativas no significa que esas tecnologías estén instaladas o configuradas en PetMatch.

---

# 4. Migraciones de base de datos

PetMatch usa:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

Para un entorno con despliegues controlados podría ser útil versionar cambios de schema mediante Flyway o Liquibase.

Eso permitiría responder con precisión:

```text
qué cambio ocurrió
en qué orden
cómo reproducirlo en otro ambiente
```

PetMatch no contiene migraciones Flyway/Liquibase.

---

# 5. Testcontainers

Las pruebas `@SpringBootTest` dependen de:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Una alternativa sería levantar una instancia temporal de MySQL durante los tests mediante Testcontainers.

Posibles beneficios:

```text
entorno reproducible
motor equivalente entre equipos
aislamiento
```

Costos:

```text
runtime de contenedores
más tiempo de arranque
más infraestructura de testing
```

PetMatch no contiene `@Testcontainers`, `@Container` ni `MySQLContainer`.

---

# 6. Perfil específico de test

No hay un `application-test.yaml` que sustituya el datasource para pruebas.

Si fuese necesario separar claramente configuraciones de desarrollo, test y producción, podría introducirse un perfil específico y combinarlo con una estrategia de base de datos controlada.

---

# 7. Optimistic locking

El punto concurrente más delicado, aceptar una postulación, utiliza un lock pesimista sobre `SupportRequest`.

Otra estrategia posible en sistemas con un patrón de concurrencia diferente sería:

```java
@Version
private Long version;
```

La diferencia conceptual es:

```text
pessimistic locking
→ bloquear antes de competir

optimistic locking
→ permitir competir y detectar una versión obsoleta al escribir
```

Las Entities de PetMatch no utilizan `@Version`.

---

# 8. Prueba concurrente real

Los tests actuales verifican el algoritmo de aceptación y el flujo integrado, pero no lanzan dos transacciones simultáneas intentando aceptar postulaciones distintas para la misma solicitud.

Una práctica avanzada podría comprobar:

```text
transacción A → acepta application B
transacción B → acepta application C

resultado esperado:
máximo una ACCEPTED
```

Ese test no existe en la suite estudiada.

---

# 9. Más cobertura REST y MVC

`RestApiIntegrationTests` demuestra explícitamente, entre otros casos:

```text
API anónima → 401
API con Basic → 200
POST Pet → 201 + Location
validation inválida → 400 ProblemDetail
```

Podrían añadirse pruebas específicas para:

```text
404 por ownership/visibilidad
409 por regla de negocio
JSON ilegible
PUT / DELETE
cancel / complete
apply / accept / reject
```

También podría construirse una suite MockMvc más amplia para el flujo HTML, incluyendo redirects, validación MVC y CSRF.

`MvpFlowIntegrationTests` no es un test end-to-end de navegador: llama Services directamente.

---

# 10. OpenAPI / Swagger

La API se explica mediante sus Controllers, DTO y capítulos Markdown.

Si otros equipos necesitaran consumirla, podría ser útil una especificación OpenAPI para documentar de forma navegable y machine-readable:

```text
rutas
request schemas
response schemas
status HTTP
```

PetMatch no tiene Swagger UI ni una especificación OpenAPI generada.

---

# 11. La asimetría de `Location` en SupportApplication

Al crear una postulación REST se genera un `Location` hacia:

```text
/api/v1/support-applications/{id}
```

pero no existe un endpoint:

```text
GET /api/v1/support-applications/{id}
```

Posibles decisiones serían:

```text
crear el GET individual
cambiar Location a un recurso navegable
reconsiderar si Location es necesaria
```

El capítulo no elige una de ellas; muestra una decisión de API que podría revisarse.

---

# 12. Mapping automático

PetMatch usa un `ApiDtoMapper` explícito y manual.

Si el número de DTO creciera considerablemente podría evaluarse MapStruct. El mapping manual, sin embargo, tiene ventajas para este proyecto:

```text
es visible
es fácil de depurar
muestra claramente las fronteras
no introduce otra herramienta
```

PetMatch no usa MapStruct ni ModelMapper.

---

# 13. Objetos de aplicación neutrales

La API convierte algunos API DTO hacia Form DTO antes de llegar al Service.

Si aparecieran más interfaces, nombres como `PetForm` podrían dejar de representar bien una entrada compartida. Una alternativa sería introducir objetos como:

```text
CreatePetCommand
UpdatePetCommand
CreateSupportRequestCommand
```

Entonces MVC y REST podrían convertir sus DTO hacia comandos neutrales.

Es una alternativa arquitectónica, no una pieza existente de PetMatch.

---

# 14. Abstracción de usuario autenticado

Los Services reciben `Authentication` y delegan en `UserService.getCurrentUser(...)`.

Si muchas capas quedaran demasiado acopladas al tipo de Spring Security, podría evaluarse una abstracción como:

```text
CurrentUser
CurrentUserProvider
AuthenticatedUser
```

Para el tamaño de PetMatch, la solución directa resulta fácil de seguir y evita una abstracción adicional sin necesidad demostrada.

---

# 15. OAuth2 / JWT

La API utiliza:

```text
HTTP Basic
+
STATELESS
```

Un escenario con una app móvil, SPA desacoplada, integraciones de terceros, scopes o un authorization server externo podría justificar evaluar OAuth2 Resource Server y tokens de acceso JWT.

JWT no es automáticamente “más seguro”; introduce decisiones sobre expiración, revocación, scopes, claves y operación.

PetMatch no contiene un JWT service, JWT filter, Resource Server ni Authorization Server.

---

# 16. Login social, MFA y method security

Otras posibilidades del ecosistema incluyen:

```text
OAuth2 Login / OpenID Connect
MFA / passkeys
@PreAuthorize
```

Ninguna forma parte del flujo de autenticación y ownership explicado en PetMatch.

Las reglas principales de recursos se resuelven mediante Services y consultas filtradas por ownership.

---

# 17. Contenedores y CI/CD

Si un equipo necesitara un runtime reproducible podría evaluar un `Dockerfile` y una composición local para aplicación + MySQL.

Si necesitara automatizar verificaciones al hacer push o pull request, una pipeline podría ejecutar:

```text
checkout
→ configurar Java
→ test
→ package
→ publicar artefacto
```

PetMatch no requiere Docker y no contiene una pipeline CI/CD que forme parte del recorrido.

---

# 18. Observabilidad

En un sistema operado en producción podrían aparecer necesidades como:

```text
logs estructurados
metrics
health checks
tracing
alertas
```

Spring Boot Actuator y herramientas externas podrían evaluarse según el entorno. No deben asumirse dashboards, Prometheus, Grafana o tracing distribuido en PetMatch.

---

# 19. Errores de API más ricos

`ApiExceptionHandler` utiliza `ProblemDetail` con `status`, `title`, `detail` y `errors` para validación.

Consumidores externos podrían justificar propiedades adicionales como:

```text
errorCode estable
correlationId
link de documentación
```

La implementación estudiada no añade esas propiedades ni un `@ExceptionHandler(Exception.class)` genérico.

---

# 20. Paginación, filtros y cache

Si los volúmenes crecieran podrían evaluarse:

```text
Pageable / Page / Slice
filtros por fecha o tipo
ordenamiento
Spring Cache / Redis
```

Estas soluciones solo deberían introducirse después de identificar un problema de volumen o rendimiento. Cache, en particular, añade decisiones de invalidación y consistencia.

La API de PetMatch no se documenta como paginada y el proyecto no usa Redis.

---

# 21. Eventos, notificaciones y mensajería

Transiciones como:

```text
application accepted
request completed
```

podrían generar necesidades futuras de email, notificaciones in-app o procesos asíncronos.

Eso podría llevar a eventos de aplicación y, si realmente fuese necesario, a infraestructura de mensajería como RabbitMQ o Kafka.

PetMatch no contiene un sistema de notificaciones, broker, producer o consumer.

---

# 22. Microservicios

PetMatch es una aplicación Spring Boot monolítica con un dominio pequeño y cohesivo.

Dividirla en microservicios agregaría:

```text
red
contratos distribuidos
múltiples despliegues
consistencia entre servicios
seguridad entre servicios
observabilidad distribuida
más operación
```

No existe una progresión obligatoria como:

```text
REST → JWT → microservicios → Docker → Kubernetes
```

La arquitectura debe responder a requisitos, no a una lista de tecnologías.

---

# 23. SPA y WebFlux

La interfaz actual usa Spring MVC + Thymeleaf con rendering del lado servidor.

Una SPA React/Vue/Angular podría tener sentido si existiera una necesidad clara de UI cliente rica o equipos frontend/backend separados. Eso obligaría a revisar autenticación, CORS, CSRF y despliegue.

WebFlux es otra decisión independiente y solo tendría sentido ante requisitos concretos de un stack reactivo. No es una “actualización obligatoria” de Spring MVC.

PetMatch no usa SPA ni WebFlux.

---

# 24. File & Image Upload

PetMatch no implementa carga de archivos o imágenes.

No aparecen en el flujo estudiado:

```text
MultipartFile
upload endpoint
storage service
S3
MinIO
Cloudinary
```

Si alguna vez se añadiera esta capacidad habría que decidir, entre otras cosas:

```text
tipo y tamaño de archivo
validación de contenido
nombre generado por servidor
almacenamiento
ownership
acceso público/privado
borrado
protección contra archivos peligrosos
```

Por tanto una imagen asociada a una mascota no debe inferirse como funcionalidad existente solo porque sea una mejora imaginable.

---

# 25. Cambios que alterarían el dominio

Ideas como estas no son pequeñas mejoras técnicas:

```text
tienda e inventario
pagos
chat
geolocalización
red social
adopciones
```

Cada una introduciría nuevos casos de uso, modelos, seguridad, estados y pruebas.

`SupportApplication`, por ejemplo, representa una postulación para ayudar con una solicitud; no es una solicitud de adopción.

---

# 26. Administración y permisos

`SecurityConfig` contiene una regla para `/admin/**` y el enum `Role` incluye `ADMIN`.

Eso no implica que exista un módulo administrativo completo con dashboard, gestión de usuarios o moderación.

Una futura área administrativa debería partir de casos de uso concretos antes de crear Controllers o vistas.

---

# 27. Evoluciones de estados y modelo

La máquina actual tiene reglas concretas. Cambios como:

```text
cancelar después de IN_PROGRESS
reabrir CANCELLED o COMPLETED
aceptar múltiples helpers
convertir SupportType en catálogo dinámico
```

requieren rediseñar estados, reglas, UI y tests. No deberían implementarse como simples cambios aislados.

---

# 28. Fechas y zonas horarias

`serviceDate` utiliza `LocalDateTime`, que no incluye zona horaria.

Si la aplicación atendiera usuarios en múltiples zonas habría que definir explícitamente una política temporal y evaluar tipos como:

```text
Instant
OffsetDateTime
ZonedDateTime
```

No debe asumirse que el valor actual se almacena automáticamente en UTC o en una zona geográfica concreta.

---

# 29. Qué evoluciones son buenas prácticas de aprendizaje

Sin transformar el producto, algunas extensiones útiles para profundizar Spring podrían ser:

```text
1. añadir un test REST de 404
2. añadir un test REST de 409
3. separar configuración de test
4. introducir migraciones controladas
5. probar concurrencia real de accept
6. documentar la API con OpenAPI
```

Cada ejercicio debería empezar explicando qué problema técnico pretende explorar.

---

# 30. Evolución incremental

Un crecimiento razonable puede verse así:

```text
problema: tests de integración poco reproducibles
→ evaluar Testcontainers

problema: schema necesita historial controlado
→ evaluar Flyway/Liquibase

problema: API consumida por otro equipo
→ evaluar OpenAPI

problema: clientes desacoplados requieren tokens/scopes
→ evaluar OAuth2/JWT
```

Cada paso responde a una necesidad distinta.

---

# 31. 🧪 Actividad de arquitectura

Escoge tres alternativas y completa:

| Evolución | Problema que resuelve | Cambio mínimo | Riesgo nuevo | Test necesario |
|---|---|---|---|---|
| Testcontainers | DB de test reproducible | dependencia + configuración | runtime de contenedores | integración |
| ... | ... | ... | ... | ... |

Después responde:

> ¿la implementarías ahora o esperarías a tener evidencia de necesidad?

Una decisión válida puede ser **no agregar todavía la complejidad**.

---

# 32. 🧪 Comprueba que entendiste

1. ¿PetMatch usa Flyway o Liquibase?
2. ¿Usa Testcontainers?
3. ¿Usa JWT/OAuth2?
4. ¿Tiene `@Version`?
5. ¿Usa MapStruct?
6. ¿Tiene Swagger UI?
7. ¿Tiene una SPA React/Vue/Angular?
8. ¿Implementa File & Image Upload?
9. ¿Tiene un módulo admin completo?
10. ¿Es una arquitectura de microservicios?
11. ¿Los tests actuales ejecutan una carrera concurrente real?
12. ¿Existe un GET individual de SupportApplication en la API?
13. ¿Qué debe existir antes de añadir una nueva tecnología?

### Respuestas esperadas

1. No.
2. No.
3. No.
4. No.
5. No.
6. No.
7. No.
8. No.
9. No.
10. No.
11. No.
12. No.
13. Un problema o requisito concreto que justifique el cambio y permita evaluar costo, alternativa y pruebas.

---

# 33. ✅ Qué debes recordar

- **Una posibilidad tecnológica no es una capacidad de PetMatch.**
- Una herramienta nueva debe justificar un problema real.
- `ddl-auto: update` podría sustituirse por migraciones controladas si el contexto lo exige.
- Las pruebas podrían ganar reproducibilidad con una estrategia de DB específica, como Testcontainers.
- `accept(...)` usa locking pesimista; no hay `@Version`.
- `ApiDtoMapper` es manual; MapStruct es solo una alternativa posible.
- La API usa HTTP Basic stateless; no JWT/OAuth2.
- PetMatch no implementa upload de archivos, SPA, WebFlux, microservicios, chat, pagos, geolocalización o una red social completa.
- El diseño actual debe evaluarse por las necesidades que resuelve, no por cuántas tecnologías incorpora.

---

# Referencia rápida del bloque

```text
36 Git, GitHub y versionado
→ conservar, revisar y compartir cambios

37 Glosario
→ recuperar el significado de un término

38 Índice
→ localizar clases y conceptos

39 Evoluciones
→ evaluar alternativas sin confundirlas con las capacidades de PetMatch
```

Puedes volver a estos capítulos de forma no lineal según la duda que tengas.

---

[← Capítulo 38 — Índice de clases y conceptos](38-indice-de-clases-y-conceptos.md) · [Índice del bloque](README.md) · [Índice general](../README.md)
