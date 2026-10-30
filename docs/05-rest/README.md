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

Este bloque estudia la segunda interfaz: endpoints HTTP, DTO JSON, seguridad REST y manejo estructurado de errores.

> **Idea central:** REST no duplica las reglas del dominio. Los Controllers REST reutilizan los mismos Services que la interfaz MVC.

Además de estudiar la implementación real de PetMatch, al terminar el bloque realizarás una [práctica de transferencia REST](practica-transferencia-rest.md) sobre `BookShelf` para demostrar que puedes reproducir el patrón con otro dominio.

---

## Qué aprenderás en este bloque

Al finalizar deberías poder explicar:

- qué diferencia hay entre `@Controller` y `@RestController`;
- cómo se construyen las rutas `/api/v1/...`;
- qué papel cumplen `GET`, `POST`, `PUT` y `DELETE` en la API;
- cómo funcionan `@PathVariable`, `@RequestBody` y `@Valid`;
- por qué PetMatch separa API Request DTO y API Response DTO;
- por qué los DTO REST son Java `record`;
- cómo `ApiDtoMapper` adapta API DTO ↔ Form DTO/Entity;
- por qué no se devuelven Entities directamente;
- cómo se expresan `200 OK`, `201 Created`, `204 No Content` y `Location`;
- por qué `/api/**` usa HTTP Basic + `STATELESS`;
- por qué CSRF está deshabilitado únicamente en la chain API;
- cómo la autenticación REST sigue desembocando en ownership de Services;
- por qué un recurso ajeno puede terminar como `404` y no necesariamente `403`;
- cómo `ApiExceptionHandler` usa `ProblemDetail`;
- cómo PetMatch diferencia 400, 401, 404 y 409;
- qué comprueban las pruebas REST existentes;
- cómo construir un contrato REST equivalente en otra aplicación sin duplicar Services;
- cómo verificar manualmente una API mediante `curl`/`curl.exe` y distinguir qué capa produce cada respuesta.

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

Después de estudiar 27–30 completa:

**[Práctica de transferencia — REST, JSON y `ProblemDetail`](practica-transferencia-rest.md)**

La práctica conserva el mismo principio del bloque:

```text
interfaz nueva
≠
reglas nuevas duplicadas
```

---

# Mapa del bloque

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
falta Basic → 401
JSON no convertible → 400
Bean Validation → 400 + errors
recurso no encontrado/visible → 404
conflicto de estado/regla/integridad → 409
```

---

# Controllers REST

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

# Endpoints

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

# Seguridad REST

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

---

# Contrato de errores

`ApiExceptionHandler` está limitado al paquete API y traduce errores seleccionados a `ProblemDetail`.

| Situación | Resultado |
|---|---|
| falta autenticación Basic | `401` por Spring Security |
| validation failure | `400 Validation failed` + `errors` |
| body JSON ilegible | `400 Invalid request body` |
| resource not found/visible | `404 Resource not found` |
| conflicto de negocio/estado/integridad | `409 Business rule conflict` |

---

# Evidencia en pruebas

`RestApiIntegrationTests` comprueba explícitamente:

```text
GET /api/v1/pets sin Basic → 401
GET /api/v1/pets con Basic válido → 200
POST /api/v1/pets válido → 201 + Location + JSON
POST /api/v1/pets inválido → 400 + ProblemDetail + errors
```

No hay en esa clase una prueba dedicada para cada comportamiento del handler; la existencia de una implementación y la existencia de cobertura específica son afirmaciones distintas.

La práctica de transferencia de este bloque utiliza comprobaciones manuales con `curl`; la estrategia de pruebas automatizadas se estudia en el bloque siguiente.

---

# Resultado esperado del bloque

## 1. Comprender PetMatch

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

Y también explicar un fallo de negocio que termina en `409 ProblemDetail`.

## 2. Demostrar transferencia

Después completa la [práctica REST de `BookShelf`](practica-transferencia-rest.md) y explica sin copiar PetMatch:

```text
POST /api/v1/books
↓
BookApiRequest
↓
@Valid
↓
ApiDtoMapper
↓
BookForm
↓
BookService
↓
BookApiResponse
↓
201 + Location + JSON
```

También deberías poder justificar:

```text
por qué MVC y REST reutilizan Service
por qué Entity no es el contrato JSON
por qué Request DTO y Response DTO pueden diferir
qué diferencia 400, 401, 404 y 409
qué error produce SecurityFilterChain
qué error traduce ApiExceptionHandler
por qué ownership sigue en Service
```

---

# Comienza aquí

**[Capítulo 27 — REST API →](27-rest-api.md)**

Cuando termines el capítulo 30:

**[Práctica de transferencia — REST, JSON y `ProblemDetail` →](practica-transferencia-rest.md)**

Después continúa con:

**[Bloque 06 — Calidad y recorrido completo →](../06-calidad-y-recorrido/README.md)**

---

[← Bloque 04 — Seguridad](../04-seguridad/README.md) · [Índice general](../README.md) · [Capítulo 27 →](27-rest-api.md) · [Práctica REST](practica-transferencia-rest.md) · [Siguiente bloque → Calidad y recorrido](../06-calidad-y-recorrido/README.md)
