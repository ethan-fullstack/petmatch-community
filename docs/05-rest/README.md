# Bloque 05 — REST

PetMatch Community ofrece dos interfaces sobre la misma lógica de negocio:

```text
Browser
→ MVC Controller
→ Service
→ Thymeleaf
→ HTML
```

Y:

```text
Cliente HTTP
→ REST Controller
→ Service
→ API DTO
→ JSON
```

Este bloque estudia la segunda interfaz completa: endpoints HTTP, DTO JSON, seguridad REST y manejo estructurado de errores.

> **Idea central:** REST no duplica las reglas del dominio. Los Controllers REST reutilizan los mismos Services que la interfaz MVC.

---

## Qué aprenderás en este bloque

Al finalizar deberías poder explicar, usando código real de PetMatch:

- qué diferencia hay entre `@Controller` y `@RestController`;
- cómo se construyen las rutas `/api/v1/...`;
- qué papel cumplen `GET`, `POST`, `PUT` y `DELETE` en la API actual;
- cómo funcionan `@PathVariable`, `@RequestBody` y `@Valid`;
- por qué PetMatch separa API Request DTO y API Response DTO;
- por qué los DTO REST son Java `record`;
- cómo `ApiDtoMapper` adapta API DTO ↔ Form DTO/Entity;
- cómo se aplana una relación JPA en JSON;
- por qué no se devuelven Entities directamente;
- cómo se expresan `200 OK`, `201 Created`, `204 No Content` y `Location`;
- por qué `/api/**` usa HTTP Basic + `STATELESS`;
- por qué CSRF está deshabilitado únicamente en la chain API;
- cómo la autenticación REST sigue desembocando en ownership de Services;
- por qué un recurso ajeno puede terminar como `404` y no necesariamente `403`;
- cómo `ApiExceptionHandler` usa `ProblemDetail`;
- cómo PetMatch diferencia 400, 401, 404 y 409;
- qué parte de esos errores pertenece a Spring Security y cuál al `@RestControllerAdvice`;
- qué comprueban las pruebas REST existentes y qué cobertura todavía no existe.

---

## Prerrequisitos

Antes de este bloque conviene estudiar:

- [Service y reglas de negocio](../02-dominio-y-persistencia/13-service-y-reglas-de-negocio.md);
- [Lazy loading y `EntityGraph`](../02-dominio-y-persistencia/17-lazy-loading-y-entitygraph.md);
- [Spring MVC](../03-web-mvc/18-spring-mvc.md);
- [Form DTO](../03-web-mvc/20-formularios-y-form-dto.md);
- [Validación](../03-web-mvc/21-validacion.md);
- [Spring Security](../04-seguridad/23-spring-security.md);
- [Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md);
- [CSRF, sesión y seguridad web](../04-seguridad/26-csrf-sesion-y-seguridad-web.md).

---

# Capítulos

27. [REST API](27-rest-api.md)
28. [DTO REST, JSON y mapping](28-dto-rest-json-y-mapping.md)
29. [Seguridad REST](29-seguridad-rest.md)
30. [`ProblemDetail` y errores HTTP](30-problemdetail-y-errores-http.md)

**Estado del bloque: completo.**

---

# Mapa completo del bloque

```mermaid
flowchart TD
    A[Cliente HTTP] --> B[/api/v1/...]
    B --> C[API SecurityFilterChain]
    C --> D[HTTP Basic]
    D --> E[Authentication]
    E --> F[REST Controller]
    F --> G[API Request DTO]
    G --> H[Bean Validation]
    H --> I[ApiDtoMapper]
    I --> J[Form DTO]
    J --> K[Service]
    K --> L[Ownership + estados + reglas]
    L --> M[Repository / DB]
    M --> N[Entity]
    N --> O[ApiDtoMapper]
    O --> P[API Response DTO]
    P --> Q[JSON]
```

En errores:

```text
falta Basic
→ Spring Security
→ 401

JSON no convertible
→ HttpMessageNotReadableException
→ ApiExceptionHandler
→ 400

Bean Validation
→ MethodArgumentNotValidException
→ ApiExceptionHandler
→ 400 + errors

NotFound de recurso/visibilidad
→ ApiExceptionHandler
→ 404

conflicto de estado/regla/integridad
→ ApiExceptionHandler
→ 409
```

---

# Controllers REST reales

Ruta:

```text
src/main/java/com/petmatch/community/controller/api/
```

Contiene:

```text
PetRestController
SupportRequestRestController
SupportApplicationRestController
ApiExceptionHandler
```

Los tres Controllers llaman a los mismos Services usados por MVC.

---

# Endpoints reales

## Mascotas

```text
GET    /api/v1/pets
POST   /api/v1/pets
GET    /api/v1/pets/{petId}
PUT    /api/v1/pets/{petId}
DELETE /api/v1/pets/{petId}
```

## Solicitudes

```text
GET  /api/v1/support-requests
GET  /api/v1/support-requests/mine
GET  /api/v1/support-requests/{requestId}
POST /api/v1/support-requests
PUT  /api/v1/support-requests/{requestId}
POST /api/v1/support-requests/{requestId}/cancel
POST /api/v1/support-requests/{requestId}/complete
```

## Postulaciones

```text
GET  /api/v1/support-applications/mine
POST /api/v1/support-requests/{requestId}/applications
GET  /api/v1/support-requests/{requestId}/applications
POST /api/v1/support-applications/{applicationId}/accept
POST /api/v1/support-applications/{applicationId}/reject
```

---

# Entrada y salida

La API no usa las Entities como contrato público.

Ejemplo Pet:

```text
JSON
↓
PetApiRequest
↓
ApiDtoMapper.toPetForm
↓
PetForm
↓
PetService
↓
Pet
↓
ApiDtoMapper.toPetResponse
↓
PetApiResponse
↓
JSON
```

El mismo patrón existe para `SupportRequest` y `SupportApplication`.

---

# Seguridad REST actual

`SecurityConfig` define:

```java
.securityMatcher("/api/**")
.authorizeHttpRequests(authorize -> authorize
    .anyRequest().authenticated()
)
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
.csrf(csrf -> csrf.disable())
.httpBasic(Customizer.withDefaults());
```

Por tanto:

```text
/api/**
→ HTTP Basic
→ authenticated
→ STATELESS
→ CSRF disabled
```

Esto no elimina ownership ni reglas de negocio.

Después del filtro:

```text
Authentication
→ UserService.getCurrentUser
→ current User
→ Service/Repository ownership
```

[Capítulo 29 — Seguridad REST](29-seguridad-rest.md)

---

# Contrato de errores

`ApiExceptionHandler` está limitado a:

```text
com.petmatch.community.controller.api
```

Y traduce errores seleccionados a `ProblemDetail`.

| Situación | Resultado |
|---|---|
| falta autenticación Basic | `401` por Spring Security |
| validation failure | `400 Validation failed` + `errors` |
| body JSON ilegible | `400 Invalid request body` |
| resource not found/visible | `404 Resource not found` |
| conflicto de negocio/estado/integridad | `409 Business rule conflict` |

[Capítulo 30 — `ProblemDetail` y errores HTTP](30-problemdetail-y-errores-http.md)

---

# Evidencia en pruebas

`RestApiIntegrationTests` comprueba explícitamente:

```text
GET /api/v1/pets sin Basic
→ 401

GET /api/v1/pets con Basic válido
→ 200

POST /api/v1/pets válido
→ 201 + Location + JSON

POST /api/v1/pets inválido
→ 400 + ProblemDetail + errors
```

No se observan en esa clase pruebas REST dedicadas para todos los casos 404, 409 o body JSON ilegible. Esos comportamientos sí están implementados en `ApiExceptionHandler`, pero no deben documentarse como cobertura automatizada ya existente.

---

# Qué NO implementa actualmente la API

No se debe presentar como implementado:

- JWT;
- OAuth2 Resource Server;
- Bearer tokens;
- API keys;
- refresh tokens;
- OpenAPI/Swagger configurado;
- HATEOAS;
- GraphQL;
- WebFlux;
- paginación;
- PATCH;
- ETags;
- rate limiting;
- CORS personalizado;
- MapStruct/ModelMapper;
- un frontend SPA separado.

---

# Resultado esperado del bloque

Al terminar 27–30 deberías poder seguir una request completa:

```text
POST /api/v1/pets
↓
HTTP Basic
↓
Authentication
↓
PetRestController
↓
PetApiRequest
↓
@Valid
↓
ApiDtoMapper
↓
PetForm
↓
PetService
↓
current User / ownership
↓
Repository
↓
Pet Entity
↓
PetApiResponse
↓
201 Created + Location + JSON
```

Y también explicar un fallo:

```text
request autenticada
↓
Service
↓
regla de negocio inválida
↓
Exception
↓
ApiExceptionHandler
↓
409 ProblemDetail
```

---

# Continúa con

El siguiente bloque ya está disponible:

**[Bloque 06 — Calidad y recorrido completo →](../06-calidad-y-recorrido/README.md)**

Comienza con:

**[Capítulo 31 — Pruebas unitarias →](../06-calidad-y-recorrido/31-pruebas-unitarias.md)**

---

[← Bloque 04 — Seguridad](../04-seguridad/README.md) · [Índice general](../README.md) · [Capítulo 27 →](27-rest-api.md) · [Siguiente bloque → Calidad y recorrido](../06-calidad-y-recorrido/README.md)
