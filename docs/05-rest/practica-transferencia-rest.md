# Práctica de transferencia — REST, JSON y `ProblemDetail`

Esta práctica acompaña los capítulos 27–30 y continúa `BookShelf`, el proyecto secundario utilizado para comprobar transferencia.

Hasta ahora la aplicación ya tiene:

```text
Spring Boot
→ JPA/MySQL
→ Services
→ Web MVC + Thymeleaf
→ validación
→ registro y login
→ Spring Security
→ ownership
```

Ahora añadiremos una **segunda interfaz** sobre la misma lógica.

La pregunta central es:

> **¿Puedes exponer los mismos casos de uso mediante HTTP + JSON sin duplicar las reglas que ya viven en los Services?**

---

## Qué vas a construir

La API quedará bajo:

```text
/api/v1
```

Endpoints principales:

```text
GET  /api/v1/books
GET  /api/v1/books/{bookId}
POST /api/v1/books
PUT  /api/v1/books/{bookId}

GET  /api/v1/loans/mine
POST /api/v1/loans/{loanId}/return
```

La API utilizará:

```text
@RestController
@RequestBody
@Valid
records para DTO REST
ResponseEntity
HTTP Basic
STATELESS
ProblemDetail
```

No construiremos todavía una suite completa de pruebas REST. Eso pertenece al bloque de Calidad.

---

# 1. Relación con los capítulos 27–30

| Capítulo | Concepto | Aplicación en `BookShelf` |
|---|---|---|
| 27 | REST API | rutas `/api/v1`, verbos HTTP, status y `Location` |
| 28 | DTO REST, JSON y mapping | `BookApiRequest`, `BookApiResponse`, `LoanApiResponse`, mapper |
| 29 | seguridad REST | HTTP Basic, `STATELESS`, `/api/**`, ownership en Service |
| 30 | errores HTTP | `ProblemDetail`, 400, 401, 404 y 409 |

La práctica sigue el mismo patrón conceptual de PetMatch, pero con libros y préstamos.

---

# 2. Lo primero que debes evitar: duplicar el negocio

Ya existe una interfaz web:

```text
Browser
→ BookController
→ BookService
→ Repository
```

La nueva interfaz será:

```text
Cliente HTTP
→ BookRestController
→ BookService
→ Repository
```

Observa el centro compartido:

```text
BookService
LoanService
```

No crees:

```text
BookApiService
LoanApiService
```

solo porque la entrada ahora sea JSON.

La interfaz cambia; las reglas centrales no deberían duplicarse.

---

# 3. Verifica el proyecto antes de empezar

Ejecuta:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

Después arranca la aplicación y confirma que la interfaz web existente todavía funciona.

La práctica REST se construye **encima** de lo anterior, no reemplazándolo.

---

# 4. Define el contrato antes del Controller

Para libros queremos recibir:

```json
{
  "title": "Effective Java",
  "isbn": "9780134685991",
  "categoryId": 1
}
```

Y responder con algo parecido a:

```json
{
  "id": 12,
  "title": "Effective Java",
  "isbn": "9780134685991",
  "status": "AVAILABLE",
  "categoryId": 1,
  "categoryName": "Backend"
}
```

Es importante notar que entrada y salida **no tienen por qué ser el mismo tipo**.

---

# 5. Request DTO y Response DTO son contratos diferentes

## Request

Responde:

> ¿Qué datos acepta el cliente para esta operación?

```text
title
isbn
categoryId
```

## Response

Responde:

> ¿Qué datos decide exponer la API?

```text
id
title
isbn
status
categoryId
categoryName
```

No uses una Entity como atajo para ambos contratos.

---

# 6. Crea el paquete de DTO REST

Ruta sugerida:

```text
src/main/java/com/example/bookshelf/dto/api/
```

Crearemos:

```text
BookApiRequest.java
BookApiResponse.java
LoanApiResponse.java
ApiDtoMapper.java
```

---

# 7. Crea `BookApiRequest`

```java
package com.example.bookshelf.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookApiRequest(
    @NotBlank
    @Size(max = 180)
    String title,

    @NotBlank
    @Size(max = 32)
    String isbn,

    @NotNull
    Long categoryId
) {
}
```

¿Por qué un `record` funciona bien aquí?

Porque el DTO representa principalmente un conjunto de datos inmutable para transportar entrada.

No significa que todos los DTO Java deban ser records.

---

# 8. Crea `BookApiResponse`

```java
package com.example.bookshelf.dto.api;

import com.example.bookshelf.model.enums.BookStatus;

public record BookApiResponse(
    Long id,
    String title,
    String isbn,
    BookStatus status,
    Long categoryId,
    String categoryName
) {
}
```

Este contrato no expone:

```text
Category Entity completa
Loans internos
campos técnicos de Hibernate
```

La API decide explícitamente qué representa su respuesta.

---

# 9. Crea `LoanApiResponse`

```java
package com.example.bookshelf.dto.api;

import com.example.bookshelf.model.enums.LoanStatus;

import java.time.LocalDateTime;

public record LoanApiResponse(
    Long id,
    Long bookId,
    String bookTitle,
    LocalDateTime borrowedAt,
    LocalDateTime dueAt,
    LocalDateTime returnedAt,
    LoanStatus status
) {
}
```

No exponemos `LibraryUser` completo.

La ruta `/loans/mine` ya representa préstamos del principal autenticado.

---

# 10. JSON no es la Entity

Cuando Jackson serializa:

```java
BookApiResponse
```

obtienes JSON.

Pero conceptualmente:

```text
Entity
≠
DTO
≠
JSON
```

Son capas distintas:

```text
Book
→ modelo persistente

BookApiResponse
→ contrato Java de salida

JSON
→ representación HTTP serializada
```

---

# 11. Reutiliza `BookForm` para el Service existente

La interfaz MVC ya llama algo equivalente a:

```java
bookService.create(BookForm form)
```

No queremos reescribir esas reglas.

Por eso el mapper puede adaptar:

```text
BookApiRequest
→ BookForm
→ BookService
```

---

# 12. Crea `ApiDtoMapper`

```java
package com.example.bookshelf.dto.api;

import com.example.bookshelf.dto.book.BookForm;
import com.example.bookshelf.model.Book;
import com.example.bookshelf.model.Loan;

public final class ApiDtoMapper {

    private ApiDtoMapper() {
    }

    public static BookForm toBookForm(BookApiRequest request) {
        BookForm form = new BookForm();
        form.setTitle(request.title());
        form.setIsbn(request.isbn());
        form.setCategoryId(request.categoryId());
        return form;
    }

    public static BookApiResponse toBookResponse(Book book) {
        return new BookApiResponse(
            book.getId(),
            book.getTitle(),
            book.getIsbn(),
            book.getStatus(),
            book.getCategory().getId(),
            book.getCategory().getName()
        );
    }

    public static LoanApiResponse toLoanResponse(Loan loan) {
        return new LoanApiResponse(
            loan.getId(),
            loan.getBook().getId(),
            loan.getBook().getTitle(),
            loan.getBorrowedAt(),
            loan.getDueAt(),
            loan.getReturnedAt(),
            loan.getStatus()
        );
    }
}
```

---

# 13. El mapper no debe convertirse en Service

El mapper puede hacer:

```text
copiar/adaptar datos
cambiar una representación por otra
```

No debería decidir:

```text
si ISBN duplicado está permitido
si un usuario puede devolver un Loan
si un Book puede prestarse
```

Esas siguen siendo reglas de Service.

---

# 14. La respuesta también necesita un fetch plan correcto

`toBookResponse(...)` navega:

```text
book.category.id
book.category.name
```

Por eso `BookService.findAll()` y `findById()` deben seguir utilizando consultas que preparen `category` cuando `open-in-view: false`.

REST no elimina el problema de LAZY.

Solo cambia quién consume la Entity antes de convertirla a salida.

---

# 15. Crea `BookRestController`

Ruta:

```text
src/main/java/com/example/bookshelf/controller/api/BookRestController.java
```

```java
package com.example.bookshelf.controller.api;

import com.example.bookshelf.dto.api.BookApiRequest;
import com.example.bookshelf.dto.api.BookApiResponse;
import com.example.bookshelf.model.Book;
import com.example.bookshelf.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import static com.example.bookshelf.dto.api.ApiDtoMapper.toBookForm;
import static com.example.bookshelf.dto.api.ApiDtoMapper.toBookResponse;

@RestController
@RequestMapping("/api/v1/books")
public class BookRestController {

    private final BookService bookService;

    public BookRestController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookApiResponse> findAll() {
        return bookService.findAll().stream()
            .map(book -> toBookResponse(book))
            .toList();
    }

    @GetMapping("/{bookId}")
    public BookApiResponse findById(
        @PathVariable Long bookId
    ) {
        return toBookResponse(bookService.findById(bookId));
    }

    @PostMapping
    public ResponseEntity<BookApiResponse> create(
        @Valid @RequestBody BookApiRequest request
    ) {
        Book book = bookService.create(toBookForm(request));

        return ResponseEntity
            .created(URI.create("/api/v1/books/" + book.getId()))
            .body(toBookResponse(book));
    }

    @PutMapping("/{bookId}")
    public BookApiResponse update(
        @PathVariable Long bookId,
        @Valid @RequestBody BookApiRequest request
    ) {
        return toBookResponse(
            bookService.update(bookId, toBookForm(request))
        );
    }
}
```

---

# 16. `@RestController` cambia la salida

En MVC tenías:

```java
@Controller
```

Y retornabas:

```java
return "books/list";
```

En REST tienes:

```java
@RestController
```

Y retornas un objeto:

```java
BookApiResponse
```

Spring lo serializa al cuerpo HTTP, normalmente como JSON.

No busca un template llamado `BookApiResponse`.

---

# 17. `@RequestBody`

En:

```java
@Valid @RequestBody BookApiRequest request
```

Spring intenta:

```text
body HTTP
→ JSON
→ BookApiRequest
```

Después Bean Validation procesa el DTO.

Compara con MVC:

```text
form fields
→ @ModelAttribute BookForm
```

La fuente de datos cambia, pero ambos son contratos de entrada.

---

# 18. `201 Created` y `Location`

Al crear un recurso respondemos:

```java
ResponseEntity.created(
    URI.create("/api/v1/books/" + book.getId())
)
```

Eso produce conceptualmente:

```text
HTTP 201 Created
Location: /api/v1/books/12
```

más el body con `BookApiResponse`.

La respuesta expresa que se creó un recurso y señala su URI.

---

# 19. `200 OK` no necesita escribirse siempre

Cuando un método retorna normalmente:

```java
BookApiResponse
```

Spring responde `200 OK` por defecto para un `GET`/`PUT` exitoso.

Usa `ResponseEntity` cuando necesitas controlar explícitamente aspectos como:

```text
status
headers
body
```

No es obligatorio envolver toda respuesta solo por tratarse de REST.

---

# 20. Reutiliza ownership para los préstamos

En la práctica de Seguridad construiste:

```java
findCurrentUserLoans(Authentication authentication)
```

Y:

```java
returnLoan(
    Long loanId,
    Authentication authentication
)
```

La API debe reutilizar esos métodos.

No debe aceptar:

```text
borrowerId
```

para decidir de quién son los préstamos consultados.

La identidad viene de `Authentication`.

---

# 21. Crea `LoanRestController`

```java
package com.example.bookshelf.controller.api;

import com.example.bookshelf.dto.api.LoanApiResponse;
import com.example.bookshelf.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.bookshelf.dto.api.ApiDtoMapper.toLoanResponse;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanRestController {

    private final LoanService loanService;

    public LoanRestController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/mine")
    public List<LoanApiResponse> findMine(
        Authentication authentication
    ) {
        return loanService
            .findCurrentUserLoans(authentication)
            .stream()
            .map(loan -> toLoanResponse(loan))
            .toList();
    }

    @PostMapping("/{loanId}/return")
    public ResponseEntity<Void> returnLoan(
        @PathVariable Long loanId,
        Authentication authentication
    ) {
        loanService.returnLoan(loanId, authentication);
        return ResponseEntity.noContent().build();
    }
}
```

---

# 22. ¿Por qué `204 No Content` en devolución?

La operación cambia estado:

```text
Loan ACTIVE → RETURNED
Book LOANED → AVAILABLE
```

Pero en esta decisión de contrato no necesitamos devolver un documento nuevo.

Por eso:

```java
ResponseEntity.noContent().build()
```

expresa:

```text
operación exitosa
+
sin body
```

No significa que todo `POST` de acción deba responder `204`.

---

# 23. MVC y REST comparten Service

Ahora puedes comparar:

```text
GET /loans
→ MVC Controller
→ LoanService.findCurrentUserLoans
→ Model
→ Thymeleaf
```

con:

```text
GET /api/v1/loans/mine
→ REST Controller
→ LoanService.findCurrentUserLoans
→ LoanApiResponse
→ JSON
```

La regla de ownership está una sola vez.

---

# 24. Añade una filter chain específica para API

En la práctica anterior `BookShelf` tenía una única chain web.

Ahora evoluciona `SecurityConfig` para tener dos políticas.

Imports relevantes:

```java
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
```

---

# 25. API `SecurityFilterChain`

Agrega antes de la web:

```java
@Bean
@Order(1)
SecurityFilterChain apiSecurityFilterChain(HttpSecurity http)
    throws Exception {

    http
        .securityMatcher("/api/**")
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            )
        )
        .csrf(csrf -> csrf.disable())
        .httpBasic(Customizer.withDefaults());

    return http.build();
}
```

---

# 26. La web pasa a ser `@Order(2)`

La chain existente debe quedar identificada como la política web:

```java
@Bean
@Order(2)
SecurityFilterChain webSecurityFilterChain(HttpSecurity http)
    throws Exception {
    // misma configuración web estudiada anteriormente
    return http.build();
}
```

El orden y el matcher permiten que `/api/**` reciba la política API antes de llegar a la política web general.

---

# 27. No copies la sesión web hacia la API

Web:

```text
form login
→ sesión
→ navegador permanece autenticado
```

API de esta práctica:

```text
HTTP Basic
→ credenciales en cada request
→ STATELESS
```

No confundas:

```text
misma identidad persistida
```

con:

```text
mismo mecanismo HTTP de autenticación
```

`DatabaseUserDetailsService` puede participar en ambos.

---

# 28. ¿Por qué CSRF está deshabilitado en esta chain?

La decisión pertenece a la política REST stateless con HTTP Basic estudiada en PetMatch.

```java
.csrf(csrf -> csrf.disable())
```

está dentro de la chain que solo coincide con:

```text
/api/**
```

No conviertas esto en:

```text
“desactivar CSRF en toda aplicación Spring”
```

La web con formulario conserva su política propia.

Para profundizar en el motivo revisa el [capítulo 26](../04-seguridad/26-csrf-sesion-y-seguridad-web.md).

---

# 29. Seguridad REST no sustituye ownership

La chain responde:

```text
¿hay un usuario autenticado?
```

Pero esta operación:

```text
POST /api/v1/loans/40/return
```

requiere además:

```text
¿Loan 40 pertenece al usuario autenticado?
¿Loan 40 está ACTIVE?
```

Esas decisiones siguen en `LoanService`.

---

# 30. 401, 404 y 409 representan problemas distintos

Para `/api/v1/loans/40/return`:

```text
sin credenciales
→ 401

credenciales válidas pero Loan 40 no pertenece al usuario
→ Service no encuentra recurso owned
→ 404

Loan pertenece al usuario pero ya está RETURNED
→ conflicto de estado
→ 409
```

No reduzcas todo a `403` o `500`.

---

# 31. Crea `ApiExceptionHandler`

Ruta:

```text
src/main/java/com/example/bookshelf/controller/api/ApiExceptionHandler.java
```

```java
package com.example.bookshelf.controller.api;

import com.example.bookshelf.exception.DuplicateIsbnException;
import com.example.bookshelf.exception.LoanRuleException;
import com.example.bookshelf.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(
    basePackages = "com.example.bookshelf.controller.api"
)
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(
            HttpStatus.NOT_FOUND,
            "Resource not found",
            exception.getMessage()
        );
    }

    @ExceptionHandler({
        DuplicateIsbnException.class,
        LoanRuleException.class,
        DataIntegrityViolationException.class
    })
    ProblemDetail handleConflict(RuntimeException exception) {
        return problem(
            HttpStatus.CONFLICT,
            "Business rule conflict",
            "La operación no puede realizarse en el estado actual del recurso."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(
        MethodArgumentNotValidException exception
    ) {
        ProblemDetail detail = problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "Uno o más campos no son válidos."
        );

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error -> errors.putIfAbsent(
                error.getField(),
                error.getDefaultMessage()
            ));

        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(
        HttpMessageNotReadableException exception
    ) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request body",
            "El cuerpo JSON no tiene el formato esperado."
        );
    }

    private ProblemDetail problem(
        HttpStatus status,
        String title,
        String detail
    ) {
        ProblemDetail problem =
            ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
```

---

# 32. Limita el advice a la API

Observa:

```java
@RestControllerAdvice(
    basePackages = "com.example.bookshelf.controller.api"
)
```

La intención es que este contrato JSON de errores pertenezca a Controllers API.

No queremos que una excepción de un formulario Thymeleaf empiece accidentalmente a responder `ProblemDetail` porque añadimos un advice global demasiado amplio.

---

# 33. No agregues un handler genérico sin motivo

Evita empezar con:

```java
@ExceptionHandler(Exception.class)
```

solo para transformar cualquier fallo en un JSON “bonito”.

Eso puede ocultar errores de programación y hacer que problemas distintos parezcan iguales.

Empieza traduciendo excepciones cuyo significado HTTP puedas justificar.

---

# 34. `ProblemDetail`

Un error puede verse conceptualmente así:

```json
{
  "type": "about:blank",
  "title": "Resource not found",
  "status": 404,
  "detail": "Book not found: 999"
}
```

La representación concreta puede incluir otras propiedades estándar dependiendo del contexto.

Lo importante es que la respuesta tiene una estructura semántica, no un string improvisado.

---

# 35. Validation error con propiedad adicional

Para un request inválido, agregamos:

```java
detail.setProperty("errors", errors);
```

Así el problema puede incluir:

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "errors": {
    "title": "must not be blank",
    "categoryId": "must not be null"
  }
}
```

No dependas de que el texto exacto del mensaje sea idéntico en todos los entornos si no lo has configurado explícitamente.

---

# 36. JSON ilegible no es lo mismo que Bean Validation

Ejemplo de JSON sintácticamente/estructuralmente ilegible:

```json
{
  "title": "Book",
  "categoryId":
}
```

Puede producir:

```text
HttpMessageNotReadableException
→ 400 Invalid request body
```

En cambio:

```json
{
  "title": "",
  "isbn": "",
  "categoryId": null
}
```

puede convertirse correctamente a `BookApiRequest`, pero fallar `@Valid`:

```text
MethodArgumentNotValidException
→ 400 Validation failed
```

Ambos son `400`, pero el problema no es el mismo.

---

# 37. 401 no lo produce tu `ApiExceptionHandler`

Si faltan credenciales para `/api/**`, Spring Security intercepta la request antes de que llegue al Controller.

Por tanto:

```text
sin Basic
→ SecurityFilterChain
→ 401
```

No necesitas lanzar manualmente:

```java
throw new UnauthorizedException();
```

desde cada REST Controller.

---

# 38. Ownership puede expresarse como 404

Supongamos:

```text
Ana tiene Loan 10
Luis está autenticado
Luis pide /api/v1/loans/10/return
```

El Service consulta:

```text
findByIdAndBorrowerId(10, luisId)
```

No encuentra un recurso visible/owned para Luis y lanza `ResourceNotFoundException`.

La API termina en:

```text
404
```

Eso evita confirmar necesariamente a Luis que existe un Loan 10 perteneciente a otra persona.

No es una regla universal para todos los sistemas; es una decisión de contrato que debes poder justificar.

---

# 39. Conflicto de estado → 409

Si Ana intenta devolver dos veces su propio préstamo:

```text
primera vez
ACTIVE → RETURNED
→ éxito

segunda vez
RETURNED
→ regla impide operación
→ LoanRuleException
→ 409 Conflict
```

`409` comunica que la request es entendible, pero entra en conflicto con el estado/reglas actuales del recurso.

---

# 40. Prepara `curl`

Si conservaste el puerto `8081` de la práctica de Fundamentos:

Linux/macOS/WSL:

```bash
export BASE_URL="http://localhost:8081"
```

PowerShell:

```powershell
$baseUrl="http://localhost:8081"
```

Si cambiaste el puerto, ajusta solamente esa base.

---

# 41. Verificación — API sin credenciales

Linux/macOS/WSL:

```bash
curl -i "$BASE_URL/api/v1/books"
```

PowerShell:

```powershell
curl.exe -i "$baseUrl/api/v1/books"
```

Esperado:

```text
401 Unauthorized
```

La request no debería llegar al `BookRestController`.

---

# 42. Verificación — GET autenticado

Linux/macOS/WSL:

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  "$BASE_URL/api/v1/books"
```

PowerShell:

```powershell
curl.exe -i `
  -u "ana@example.com:TU_PASSWORD" `
  "$baseUrl/api/v1/books"
```

Esperado:

```text
200 OK
Content-Type: application/json
```

más un array JSON.

---

# 43. Verificación — crear libro

Linux/macOS/WSL:

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Domain-Driven Design",
    "isbn": "9780321125217",
    "categoryId": 1
  }' \
  "$BASE_URL/api/v1/books"
```

PowerShell:

```powershell
curl.exe -i `
  -u "ana@example.com:TU_PASSWORD" `
  -H "Content-Type: application/json" `
  -d '{"title":"Domain-Driven Design","isbn":"9780321125217","categoryId":1}' `
  "$baseUrl/api/v1/books"
```

Esperado:

```text
201 Created
Location: /api/v1/books/{id}
Content-Type: application/json
```

---

# 44. Verificación — request inválido

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  -H "Content-Type: application/json" \
  -d '{"title":"","isbn":"","categoryId":null}' \
  "$BASE_URL/api/v1/books"
```

Esperado:

```text
400 Bad Request
ProblemDetail
errors
```

Comprueba que el body permita identificar qué fields fallaron.

---

# 45. Verificación — JSON ilegible

Ejemplo deliberadamente roto:

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  -H "Content-Type: application/json" \
  -d '{"title":"Book","categoryId":}' \
  "$BASE_URL/api/v1/books"
```

Esperado:

```text
400 Bad Request
Invalid request body
```

No debería confundirse con `Validation failed`.

---

# 46. Verificación — recurso inexistente

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  "$BASE_URL/api/v1/books/999999"
```

Esperado:

```text
404 Not Found
ProblemDetail
```

si ese id no existe.

---

# 47. Verificación — conflicto por ISBN

Crea un libro y repite el mismo ISBN.

Esperado:

```text
DuplicateIsbnException
→ ApiExceptionHandler
→ 409 Conflict
```

La regla sigue estando en `BookService`.

El Controller REST no consulta Repository para descubrir el duplicado.

---

# 48. Verificación — mis préstamos

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  "$BASE_URL/api/v1/loans/mine"
```

Esperado:

```text
200 OK
```

Y solo préstamos obtenidos a partir del principal autenticado.

---

# 49. Verificación — ownership

Con dos cuentas:

```text
Ana
Luis
```

supón que `Loan 10` pertenece a Ana.

Luis intenta:

```bash
curl -i \
  -u "luis@example.com:TU_PASSWORD" \
  -X POST \
  "$BASE_URL/api/v1/loans/10/return"
```

Esperado según el patrón de esta práctica:

```text
404 Not Found
```

porque `LoanService.findOwnedLoan(...)` no encuentra el recurso dentro del ownership de Luis.

---

# 50. Verificación — devolución correcta

Ana ejecuta:

```bash
curl -i \
  -u "ana@example.com:TU_PASSWORD" \
  -X POST \
  "$BASE_URL/api/v1/loans/10/return"
```

Esperado:

```text
204 No Content
```

Y en persistencia:

```text
Loan → RETURNED
Book → AVAILABLE
```

---

# 51. Verificación — conflicto de estado

Ana intenta devolver otra vez el mismo Loan.

Esperado:

```text
409 Conflict
Business rule conflict
```

La API no debería responder `200` solo porque el endpoint existe.

---

# 52. Método HTTP no es regla de negocio

`POST` puede expresar una acción como:

```text
/loans/{id}/return
```

pero escribir:

```java
@PostMapping
```

no garantiza que la operación sea válida.

Todavía necesitas:

```text
Authentication
ownership
estado ACTIVE
transacción
```

HTTP y dominio se complementan; no se sustituyen.

---

# 53. No devuelvas Entity directamente

Evita:

```java
@GetMapping
public List<Book> findAll() {
    return bookService.findAll();
}
```

Aunque pueda serializarse en algunos casos, estás convirtiendo tu modelo persistente en contrato público accidental.

Problemas posibles:

```text
exposición de campos no deseados
relaciones bidireccionales
LAZY durante serialización
acoplamiento API ↔ Entity
cambios de schema Java alterando JSON
```

DTO REST hace explícito el contrato.

---

# 54. No uses `BookForm` como JSON público solo porque ya existe

`BookForm` fue diseñado para MVC.

La API puede necesitar:

```text
nombres diferentes
campos diferentes
versionado propio
reglas de compatibilidad
```

En esta práctica ambos contratos se parecen, pero siguen siendo conceptos distintos.

Por eso:

```text
BookApiRequest
→ mapper
→ BookForm
→ Service
```

---

# 55. No copies reglas al Controller REST

Evita:

```java
if (bookRepository.existsByIsbn(request.isbn())) {
    return ResponseEntity.status(409).build();
}
```

si esa regla ya está en `BookService`.

Eso produciría:

```text
MVC con una regla
REST con otra copia
```

que pueden divergir con el tiempo.

---

# 56. No conviertas todo error en 400

Distingue:

```text
JSON ilegible
→ 400

validation failure
→ 400

sin autenticación
→ 401

recurso no encontrado/visible
→ 404

conflicto de negocio/estado
→ 409
```

El status forma parte del contrato HTTP.

---

# 57. No devuelvas 200 para una creación solo por comodidad

Puedes técnicamente devolver un body desde `POST`, pero si el contrato representa creación de recurso y dispones de la URI, `201 Created + Location` comunica mejor el resultado.

La elección del status debe expresar lo ocurrido.

---

# 58. Checkpoint 27 — REST API

Sin mirar código, explica:

```text
POST /api/v1/books
→ ?
→ ?
→ ?
→ 201 + Location + JSON
```

Debes incluir:

```text
SecurityFilterChain
@RestController
@RequestBody
Service
ResponseEntity
```

Revisa [capítulo 27](27-rest-api.md).

---

# 59. Checkpoint 28 — DTO, JSON y mapping

Responde:

```text
¿Por qué BookApiRequest y BookApiResponse son distintos?
¿Por qué no retornamos Book Entity?
¿Qué responsabilidad tiene ApiDtoMapper?
¿Por qué el mapper no valida ISBN duplicado?
```

Después dibuja:

```text
JSON
→ Request DTO
→ Form DTO
→ Service
→ Entity
→ Response DTO
→ JSON
```

Revisa [capítulo 28](28-dto-rest-json-y-mapping.md).

---

# 60. Checkpoint 29 — Seguridad REST

Explica:

```text
/api/**
→ qué chain
→ qué autenticación
→ qué política de sesión
→ qué ocurre con CSRF
→ cómo llega Authentication al Service
```

Y responde:

> ¿Por qué HTTP Basic válido no autoriza automáticamente devolver cualquier Loan?

Revisa [capítulo 29](29-seguridad-rest.md).

---

# 61. Checkpoint 30 — `ProblemDetail`

Clasifica:

```text
sin Basic
→ 401

JSON roto
→ 400 Invalid request body

DTO inválido
→ 400 Validation failed

Book inexistente
→ 404

Loan ajeno ocultado por ownership
→ 404

ISBN duplicado
→ 409

Loan ya RETURNED
→ 409
```

Después identifica qué respuestas produce Spring Security y cuáles produce `ApiExceptionHandler`.

Revisa [capítulo 30](30-problemdetail-y-errores-http.md).

---

# 62. Prueba de transferencia final

Compara las dos interfaces.

| Necesidad | MVC | REST |
|---|---|---|
| entrada creación | `BookForm` | `BookApiRequest` |
| origen de entrada | form fields | JSON body |
| Controller | `@Controller` | `@RestController` |
| salida | view name | DTO serializado |
| éxito creación | redirect | `201 + Location` |
| validation error | `BindingResult` + template | `400 ProblemDetail` |
| autenticación | form login | HTTP Basic |
| estado auth | sesión web | stateless |
| ownership | `LoanService` | el mismo `LoanService` |
| reglas | `BookService` / `LoanService` | los mismos Services |

La columna más importante es la última: **el transporte cambia; el dominio no debería duplicarse**.

---

# 63. Qué demuestra aprendizaje

Una explicación débil sería:

> “Para hacer REST pongo `@RestController`.”

Una explicación transferible sería:

> “REST introduce otro contrato de transporte. Recibo JSON en DTO de request, valido, adapto al contrato que espera el caso de uso, reutilizo Service y convierto el resultado a DTO de response; seguridad y errores HTTP se resuelven en capas específicas.”

---

# 64. Checklist de salida

Antes de pasar a Calidad, comprueba que puedes:

- [ ] crear rutas `/api/v1/...`;
- [ ] distinguir `@Controller` de `@RestController`;
- [ ] usar `@RequestBody`;
- [ ] usar `@Valid` sobre JSON;
- [ ] crear Request DTO y Response DTO distintos;
- [ ] explicar cuándo un `record` es útil como DTO;
- [ ] mapear API DTO ↔ contrato del Service;
- [ ] evitar exponer Entities directamente;
- [ ] reutilizar los mismos Services de MVC;
- [ ] devolver `200`, `201` y `204` con intención;
- [ ] construir un header `Location`;
- [ ] configurar una chain específica para `/api/**`;
- [ ] explicar HTTP Basic;
- [ ] explicar `STATELESS`;
- [ ] explicar por qué CSRF se deshabilita solo en la chain API estudiada;
- [ ] mantener ownership dentro del Service;
- [ ] distinguir 401, 404 y 409;
- [ ] crear un `@RestControllerAdvice` limitado a la API;
- [ ] construir `ProblemDetail`;
- [ ] agregar errores de fields a `ProblemDetail`;
- [ ] distinguir JSON ilegible de Bean Validation;
- [ ] probar manualmente endpoints con `curl`/`curl.exe`;
- [ ] explicar qué capa produce cada respuesta.

---

# 65. Resultado esperado

Al terminar esta práctica debes poder seguir de extremo a extremo:

```text
POST /api/v1/books
↓
HTTP Basic
↓
API SecurityFilterChain
↓
BookRestController
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
BookRepository
↓
MySQL
↓
Book
↓
BookApiResponse
↓
201 Created + Location + JSON
```

Y también:

```text
POST /api/v1/loans/{id}/return
↓
Authentication
↓
LoanService
↓
ownership
↓
estado
↓
transacción
↓
204 o ProblemDetail
```

Si puedes construir y explicar ambos recorridos sin copiar los Controllers de PetMatch, ya estás transfiriendo el patrón REST.

---

## Continúa con

Después continúa con:

**[Bloque 06 — Calidad y recorrido completo →](../06-calidad-y-recorrido/README.md)**

---

[← Índice REST](README.md) · [Índice general](../README.md) · [Siguiente bloque → Calidad y recorrido](../06-calidad-y-recorrido/README.md)
