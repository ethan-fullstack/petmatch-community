# Práctica de transferencia — calidad, pruebas y recorrido completo

Esta práctica acompaña los capítulos 31–35 y continúa `BookShelf` en el estado alcanzado después de Seguridad y REST.

Hasta este punto el proyecto ya tiene:

```text
Spring Boot
→ JPA/MySQL
→ Services
→ MVC + Thymeleaf
→ validación
→ Spring Security
→ ownership
→ REST + JSON
→ ProblemDetail
```

Ahora la pregunta cambia:

> **¿Cómo demuestras que una regla funciona, qué frontera cubre cada test y cómo diagnosticas un fallo sin afirmar más de lo que realmente comprobaste?**

La meta no es acumular anotaciones de testing. La meta es relacionar:

```text
comportamiento esperado
→ nivel de prueba adecuado
→ preparación mínima
→ evidencia observable
→ límite de lo demostrado
```

---

## Qué vas a construir

Trabajarás con tres niveles de evidencia:

```text
LoanServiceTests
→ JUnit 5 + Mockito

BookShelfFlowIntegrationTests
→ @SpringBootTest + Services + JPA + DB

BookRestApiIntegrationTests
→ @SpringBootTest + MockMvc + Security + REST + DB
```

Después recorrerás un caso completo y practicarás diagnóstico por capas.

---

# 1. Relación con los capítulos 31–35

| Capítulo | Concepto | Aplicación en `BookShelf` |
|---|---|---|
| 31 | pruebas unitarias | aislar reglas de `LoanService` con Mockito |
| 32 | integración | comprobar Services/JPA/DB y HTTP con `MockMvc` |
| 33 | flujo completo | registrar usuario → prestar → consultar → devolver |
| 34 | decisiones | justificar diseño y trade-offs |
| 35 | errores frecuentes | diagnosticar por capa en vez de probar cambios al azar |

---

# 2. Usa el estado más reciente del flujo de préstamo

En Persistencia apareció primero:

```java
borrowBook(bookId, borrowerId, dueAt)
```

Después, en Seguridad, el caso de uso evolucionó a:

```java
borrowBook(
    Long bookId,
    LocalDateTime dueAt,
    Authentication authentication
)
```

La identidad ya no debe venir desde un `borrowerId` controlado por el cliente.

Para devolución también trabajamos con:

```java
returnLoan(
    Long loanId,
    Authentication authentication
)
```

Y ownership mediante:

```text
Authentication
→ LibraryUserService.getCurrentUser(...)
→ borrower.id
→ LoanRepository.findByIdAndBorrowerId(...)
```

Los tests de este bloque deben comprobar **esa versión**, no reintroducir el diseño anterior.

---

# 3. Antes de escribir un test, formula la afirmación

Ejemplo:

```text
Si un Loan owned ya está RETURNED,
LoanService.returnLoan(...) debe rechazar la operación.
```

Eso es más preciso que:

```text
“probar LoanService”
```

Usa mentalmente:

```text
Given
→ estado inicial

When
→ acción

Then
→ evidencia observable
```

---

# 4. No todos los tests necesitan Spring

Para comprobar:

```text
Loan RETURNED
→ returnLoan rechaza operación
```

no necesitas:

```text
ApplicationContext
MySQL
HTTP
SecurityFilterChain
Thymeleaf
```

Puedes usar:

```text
LoanService real
+
colaboradores mock
```

Ese será el primer nivel.

---

# 5. Revisa dependencias de test

Abre `pom.xml` y confirma que tu proyecto tiene las dependencias necesarias para los imports que realmente vas a utilizar.

En PetMatch con Spring Boot 4.1.1 aparecen capacidades de test para JPA, Security y Web MVC.

No copies dependencias solo porque existan en otro proyecto.

Ejecuta primero:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

Si el proyecto ya falla, resuelve esa base antes de agregar nuevos escenarios.

---

# 6. Crea `LoanServiceTests`

Ruta:

```text
src/test/java/com/example/bookshelf/service/LoanServiceTests.java
```

Ejemplo alineado con el flujo autenticado:

```java
package com.example.bookshelf.service;

import com.example.bookshelf.exception.LoanRuleException;
import com.example.bookshelf.model.Book;
import com.example.bookshelf.model.LibraryUser;
import com.example.bookshelf.model.Loan;
import com.example.bookshelf.model.enums.LoanStatus;
import com.example.bookshelf.repository.BookRepository;
import com.example.bookshelf.repository.LibraryUserRepository;
import com.example.bookshelf.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LibraryUserRepository libraryUserRepository;

    @Mock
    private LibraryUserService libraryUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoanService loanService;

    @Test
    void returnLoanRejectsLoanThatIsAlreadyReturned() {
        LibraryUser borrower = mock(LibraryUser.class);
        Loan loan = mock(Loan.class);

        when(libraryUserService.getCurrentUser(authentication))
            .thenReturn(borrower);
        when(borrower.getId()).thenReturn(20L);
        when(loanRepository.findByIdAndBorrowerId(10L, 20L))
            .thenReturn(Optional.of(loan));
        when(loan.getStatus()).thenReturn(LoanStatus.RETURNED);

        assertThrows(
            LoanRuleException.class,
            () -> loanService.returnLoan(10L, authentication)
        );

        verify(loan, never()).markReturned(any(LocalDateTime.class));
    }

    @Test
    void returnLoanUpdatesOwnedActiveLoanAndBook() {
        LibraryUser borrower = mock(LibraryUser.class);
        Loan loan = mock(Loan.class);
        Book book = mock(Book.class);

        when(libraryUserService.getCurrentUser(authentication))
            .thenReturn(borrower);
        when(borrower.getId()).thenReturn(20L);
        when(loanRepository.findByIdAndBorrowerId(10L, 20L))
            .thenReturn(Optional.of(loan));
        when(loan.getStatus()).thenReturn(LoanStatus.ACTIVE);
        when(loan.getBook()).thenReturn(book);

        loanService.returnLoan(10L, authentication);

        verify(loan).markReturned(any(LocalDateTime.class));
        verify(book).markAvailable();
    }
}
```

> [!NOTE]
> Si tu `LoanService` final eliminó una dependencia que ya no usa —por ejemplo `LibraryUserRepository` después de introducir `LibraryUserService`— elimina también ese mock. El test debe reflejar el constructor real de tu proyecto.

---

# 7. ¿Qué demuestra el primer test?

Dadas respuestas controladas:

```text
usuario actual → borrower 20
loan 10 pertenece a borrower 20
loan 10 está RETURNED
```

el Service:

```text
lanza LoanRuleException
+
no vuelve a ejecutar markReturned
```

No demuestra:

```text
SQL real
JPA real
PESSIMISTIC_WRITE real
rollback real
HTTP 409
```

---

# 8. ¿Qué demuestra el segundo test?

Dado:

```text
Loan owned
+
Loan ACTIVE
```

verifica:

```text
Loan.markReturned(...)
Book.markAvailable()
```

Esto comprueba decisiones del Service y colaboración con Entities.

No comprueba todavía que Hibernate persista esos cambios.

---

# 9. `@Mock` no ejecuta infraestructura

```java
@Mock
private LoanRepository loanRepository;
```

significa:

```text
Repository real ❌
Spring Data real ❌
Hibernate real ❌
MySQL real ❌
```

Por eso:

```java
when(loanRepository.findByIdAndBorrowerId(...))
```

no prueba la query JPA.

---

# 10. `@InjectMocks` no equivale a `@Autowired`

```java
@InjectMocks
private LoanService loanService;
```

prepara la unidad bajo prueba con los mocks disponibles.

No levanta Spring Boot.

En cambio:

```java
@Autowired
```

en una prueba con contexto solicita un Bean real al `ApplicationContext`.

---

# 11. Tres herramientas de Mockito/JUnit

```text
when(...)
→ preparar respuesta de un colaborador

verify(...)
→ comprobar una interacción relevante

assertThrows(...)
→ comprobar una excepción esperada
```

No verifiques cada getter por costumbre. Verifica aquello que representa el comportamiento que quieres demostrar.

---

# 12. Ejecuta solo la prueba unitaria

Linux/macOS/WSL:

```bash
./mvnw -Dtest=LoanServiceTests test
```

Windows:

```powershell
.\mvnw.cmd -Dtest=LoanServiceTests test
```

Si falla, lee:

```text
nombre del test
expected / actual
excepción
línea
interacción Mockito
```

antes de cambiar código productivo.

---

# 13. Siguiente nivel: Services + JPA + DB

Ahora queremos comprobar algo que Mockito no puede demostrar:

```text
Services reales
+
Repositories reales
+
JPA/Hibernate
+
DB
+
transacciones
+
estados persistidos
```

Aquí sí tiene sentido `@SpringBootTest`.

---

# 14. Dependencia de DB

Si tu configuración sigue el patrón estudiado:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Y no añadiste H2, Testcontainers o un perfil alternativo, las pruebas de integración necesitarán una DB accesible.

Linux/macOS/WSL:

```bash
export DB_URL="jdbc:mysql://localhost:3306/bookshelf"
export DB_USERNAME="root"
export DB_PASSWORD="TU_PASSWORD"
```

PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bookshelf"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="TU_PASSWORD"
```

Un fallo de conexión no demuestra un error de `LoanService`.

---

# 15. Crea `BookShelfFlowIntegrationTests`

Ruta:

```text
src/test/java/com/example/bookshelf/integration/BookShelfFlowIntegrationTests.java
```

```java
package com.example.bookshelf.integration;

import com.example.bookshelf.dto.auth.RegistrationForm;
import com.example.bookshelf.model.Book;
import com.example.bookshelf.model.Category;
import com.example.bookshelf.model.LibraryUser;
import com.example.bookshelf.model.Loan;
import com.example.bookshelf.model.enums.BookStatus;
import com.example.bookshelf.model.enums.LoanStatus;
import com.example.bookshelf.repository.BookRepository;
import com.example.bookshelf.repository.CategoryRepository;
import com.example.bookshelf.repository.LoanRepository;
import com.example.bookshelf.service.LibraryUserService;
import com.example.bookshelf.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class BookShelfFlowIntegrationTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LibraryUserService libraryUserService;

    @Autowired
    private LoanService loanService;

    @Test
    void borrowAndReturnKeepsStatusesConsistent() {
        String suffix = UUID.randomUUID()
            .toString()
            .substring(0, 8);

        Category category = categoryRepository.save(
            new Category("Backend-" + suffix)
        );

        Book book = bookRepository.save(
            new Book(
                "Effective Java",
                "isbn-" + suffix,
                category
            )
        );

        LibraryUser borrower = registerUser(
            "Reader",
            "reader-" + suffix + "@example.com"
        );

        Authentication authentication =
            authenticationFor(borrower);

        Loan loan = loanService.borrowBook(
            book.getId(),
            LocalDateTime.now().plusDays(7),
            authentication
        );

        Book loanedBook = bookRepository
            .findById(book.getId())
            .orElseThrow();
        Loan activeLoan = loanRepository
            .findById(loan.getId())
            .orElseThrow();

        assertEquals(BookStatus.LOANED, loanedBook.getStatus());
        assertEquals(LoanStatus.ACTIVE, activeLoan.getStatus());

        loanService.returnLoan(
            loan.getId(),
            authentication
        );

        Book availableBook = bookRepository
            .findById(book.getId())
            .orElseThrow();
        Loan returnedLoan = loanRepository
            .findById(loan.getId())
            .orElseThrow();

        assertEquals(BookStatus.AVAILABLE, availableBook.getStatus());
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());
    }

    private LibraryUser registerUser(String name, String email) {
        RegistrationForm form = new RegistrationForm();
        form.setName(name);
        form.setEmail(email);
        form.setPassword("testing123");
        form.setConfirmPassword("testing123");
        return libraryUserService.register(form);
    }

    private Authentication authenticationFor(LibraryUser user) {
        return new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            "ignored",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
```

---

# 16. Qué demuestra esta integración

Integra:

```text
ApplicationContext
Services
Repositories
JPA/Hibernate
DB
transacciones
Entities
```

Y comprueba secuencialmente:

```text
Book AVAILABLE
→ borrow
→ Book LOANED
→ Loan ACTIVE
→ return
→ Loan RETURNED
→ Book AVAILABLE
```

---

# 17. Qué NO demuestra

No demuestra directamente:

```text
Thymeleaf
request HTTP
SecurityFilterChain
HTTP Basic
ProblemDetail
concurrencia real entre dos transacciones
```

Aunque `borrowBook(...)` use un Repository con lock, este test es secuencial.

No lo presentes como prueba concurrente.

---

# 18. `contextLoads()` tiene un objetivo menor

```java
@SpringBootTest
class BookShelfApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Si pasa, hay evidencia de que Spring pudo construir el contexto de esa prueba.

No demuestra por sí solo:

```text
borrow
return
ownership
REST
queries específicas
```

---

# 19. Tercer nivel: HTTP con `MockMvc`

Ahora queremos incluir:

```text
SecurityFilterChain
→ HTTP mapping
→ JSON
→ validation
→ REST Controller
→ Service
→ JPA/DB
```

sin necesitar navegador ni conexión HTTP externa real.

---

# 20. Crea `BookRestApiIntegrationTests`

Ruta:

```text
src/test/java/com/example/bookshelf/integration/BookRestApiIntegrationTests.java
```

```java
package com.example.bookshelf.integration;

import com.example.bookshelf.dto.auth.RegistrationForm;
import com.example.bookshelf.model.Category;
import com.example.bookshelf.repository.CategoryRepository;
import com.example.bookshelf.service.LibraryUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookRestApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryUserService libraryUserService;

    @Autowired
    private CategoryRepository categoryRepository;

    private String email;
    private final String password = "testing123";
    private Long categoryId;

    @BeforeEach
    void prepareData() {
        String suffix = UUID.randomUUID()
            .toString()
            .substring(0, 8);

        email = "api-" + suffix + "@example.com";

        RegistrationForm form = new RegistrationForm();
        form.setName("API Reader");
        form.setEmail(email);
        form.setPassword(password);
        form.setConfirmPassword(password);
        libraryUserService.register(form);

        Category category = categoryRepository.save(
            new Category("API-" + suffix)
        );
        categoryId = category.getId();
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateBook() throws Exception {
        String body = """
            {
              "title": "Clean Architecture",
              "isbn": "test-isbn-001",
              "categoryId": %d
            }
            """.formatted(categoryId);

        mockMvc.perform(post("/api/v1/books")
                .with(httpBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.title")
                .value("Clean Architecture"))
            .andExpect(jsonPath("$.categoryId")
                .value(categoryId));
    }

    @Test
    void invalidBookReturnsProblemDetail() throws Exception {
        String body = """
            {
              "title": "",
              "isbn": "",
              "categoryId": null
            }
            """;

        mockMvc.perform(post("/api/v1/books")
                .with(httpBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title")
                .value("Validation failed"))
            .andExpect(jsonPath("$.errors.title").exists())
            .andExpect(jsonPath("$.errors.isbn").exists())
            .andExpect(jsonPath("$.errors.categoryId").exists());
    }
}
```

---

# 21. Qué integra `MockMvc`

Este escenario puede recorrer:

```text
request construida por MockMvc
→ SecurityFilterChain
→ HTTP Basic
→ REST Controller
→ JSON deserializado
→ Bean Validation
→ Service
→ Repository
→ DB
→ DTO response
→ JSON response
```

Es una frontera mucho mayor que el unit test.

---

# 22. Qué NO significa `MockMvc`

No implica automáticamente:

```text
browser real
socket TCP externo
DNS
proxy
TLS
frontend JavaScript
```

Una descripción precisa es:

> integración HTTP de la aplicación mediante `MockMvc`.

Evita llamarlo “end-to-end completo” sin explicar su frontera.

---

# 23. Ejecuta la suite

Con DB disponible:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

Si unitarias pasan e integración falla por conexión:

```text
no concluyas inmediatamente
→ “la lógica está mal”
```

Separa primero:

```text
lógica
vs
infraestructura
```

---

# 24. Tabla de fronteras

| Test | Spring | DB | HTTP mapping | Security chain | Regla Service |
|---|---:|---:|---:|---:|---:|
| `LoanServiceTests` | no | no | no | no | sí |
| `BookShelfFlowIntegrationTests` | sí | sí en esta configuración | no | no | sí |
| `BookRestApiIntegrationTests` | sí | sí en esta configuración | sí | sí | sí |

---

# 25. Flujo completo de `BookShelf`

```text
1. usuario se registra
2. password se persiste como hash
3. usuario se autentica
4. existe Category
5. existe Book AVAILABLE
6. usuario solicita préstamo
7. identidad sale de Authentication
8. LoanService bloquea Book
9. valida AVAILABLE
10. valida ausencia de Loan ACTIVE
11. Book pasa a LOANED
12. Loan nace ACTIVE
13. usuario consulta sus préstamos
14. ownership filtra por borrower
15. usuario devuelve Loan
16. Loan pasa a RETURNED
17. Book vuelve a AVAILABLE
```

---

# 26. Recorre la devolución REST

```text
POST /api/v1/loans/{id}/return
↓
API SecurityFilterChain
↓
HTTP Basic
↓
Authentication
↓
LoanRestController
↓
LoanService.returnLoan
↓
LibraryUser actual
↓
findByIdAndBorrowerId
↓
ownership
↓
Loan ACTIVE
↓
markReturned
↓
Book AVAILABLE
↓
commit
↓
204 No Content
```

Ownership fallido:

```text
Repository no encuentra Loan owned
→ ResourceNotFoundException
→ ApiExceptionHandler
→ 404 ProblemDetail
```

Estado inválido:

```text
Loan RETURNED
→ LoanRuleException
→ 409 ProblemDetail
```

---

# 27. Diagnóstico — 400

Si:

```text
POST /api/v1/books
→ 400
```

pregunta:

```text
¿JSON ilegible?
¿BookApiRequest se construyó?
¿falló @Valid?
¿entró al Controller?
```

No todo `400` significa lo mismo.

---

# 28. Diagnóstico — 401

En `/api/**` revisa:

```text
credenciales Basic
usuario existente
password correcto
usuario active
```

Un `401` normalmente ocurre antes de ejecutar el Controller REST.

---

# 29. Diagnóstico — 404

En operaciones owned:

```text
404
```

puede significar:

```text
id inexistente
```

o:

```text
id no visible/owned por el usuario actual
```

Busca qué query usa el Service.

---

# 30. Diagnóstico — 409

Pregunta:

```text
¿qué regla o estado entró en conflicto?
```

Ejemplos:

```text
ISBN duplicado
Loan ya RETURNED
Book no AVAILABLE
```

Un `409` puede ser comportamiento correcto.

---

# 31. Diagnóstico — DB en tests

Síntomas:

```text
Connection refused
Unknown database
Access denied
```

Capa probable:

```text
configuración / infraestructura
```

Revisa primero:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
MySQL
schema
```

---

# 32. Diagnóstico — LAZY

Ante `LazyInitializationException` pregunta:

```text
¿qué relación se leyó?
¿qué query debía cargarla?
¿el mapeo ocurrió después del límite transaccional?
¿falta EntityGraph para esa lectura?
```

Evita cambiar todo a `EAGER` como reacción automática.

---

# 33. Diagnóstico — Mockito

Mensajes como:

```text
Wanted but not invoked
Unnecessary stubbing
expected exception but none thrown
```

obligan a revisar:

```text
¿el escenario llegó a esa rama?
¿el stub se usa realmente?
¿verifico algo irrelevante?
¿cambió la regla del Service?
```

No uses `lenient()` como primera respuesta para silenciar el test.

---

# 34. Buenas prácticas como decisiones

| Decisión | Beneficio | Trade-off |
|---|---|---|
| constructor injection | dependencias explícitas/testables | constructores evidencian demasiadas responsabilidades |
| Form DTO separado | contrato MVC controlado | mapping adicional |
| API DTO separado | contrato JSON explícito | más tipos/mapping |
| Service compartido MVC/REST | una sola regla | exige disciplina de capas |
| `open-in-view: false` | fetch explícito | exige preparar relaciones |
| `PESSIMISTIC_WRITE` | protege competencia sobre Book | bloqueo y menor concurrencia |
| dos security chains | políticas web/API separadas | más configuración |
| `ProblemDetail` | errores REST estructurados | requiere traducciones coherentes |
| DB real en integración actual | motor real | setup local más pesado |

La pregunta útil es:

```text
problema
→ decisión
→ beneficio
→ trade-off
→ alternativa si cambia la necesidad
```

---

# 35. No agregues herramientas solo para “mejorar calidad”

Después de estudiar tests podrías considerar:

```text
Testcontainers
JaCoCo
ArchUnit
Selenium
CI/CD
```

Pero esta práctica no las convierte en requisitos.

Primero entiende la suite existente y sus huecos reales.

---

# 36. Qué cobertura puedes afirmar

Si estos tests existen y pasan, puedes afirmar algo como:

```text
reglas concretas de returnLoan tienen unit tests
borrow/return secuencial integra Service + JPA + DB
API books prueba auth, creación y validación con MockMvc
```

No puedes deducir automáticamente:

```text
100 % coverage
locking concurrente probado
todos los endpoints probados
toda la UI probada
producción validada
```

---

# 37. Checkpoint 31 — unitarias

Responde:

```text
¿Por qué LoanService es real y Repository es mock?
¿Qué hace @InjectMocks?
¿Qué diferencia hay entre when y verify?
¿Por qué el test no comprueba SQL?
```

Revisa [capítulo 31](31-pruebas-unitarias.md).

---

# 38. Checkpoint 32 — integración

Clasifica:

```text
contextLoads
BookShelfFlowIntegrationTests
BookRestApiIntegrationTests
```

Para cada uno explica:

```text
qué levanta
qué integra
qué necesita de DB
qué NO demuestra
```

Revisa [capítulo 32](32-pruebas-de-integracion.md).

---

# 39. Checkpoint 33 — flujo completo

Dibuja:

```text
register
→ Authentication
→ Book AVAILABLE
→ borrow
→ Loan ACTIVE
→ ownership
→ return
→ Loan RETURNED
→ Book AVAILABLE
```

Después recorre una operación desde HTTP hasta DB.

Revisa [capítulo 33](33-flujo-completo-petmatch.md) y compara el método de análisis, no el dominio.

---

# 40. Checkpoint 34 — decisiones

Elige tres decisiones y completa:

```text
problema
→ decisión
→ beneficio
→ trade-off
→ alternativa
```

Revisa [capítulo 34](34-buenas-practicas-y-decisiones.md).

---

# 41. Checkpoint 35 — errores frecuentes

Diagnostica primero la capa probable:

```text
API → 401
API → 409 al repetir return
integration test → no conecta a DB
template → falla leyendo category
Mockito → Wanted but not invoked
```

Revisa [capítulo 35](35-errores-frecuentes.md).

---

# 42. Prueba final de transferencia

Debes poder describir:

```text
LoanServiceTests
→ Service real + mocks

BookShelfFlowIntegrationTests
→ Spring + Service + JPA + DB

BookRestApiIntegrationTests
→ Security + REST + Validation + Service + DB mediante MockMvc
```

Y añadir con precisión:

> **Ninguna de esas pruebas demuestra por sí sola una carrera concurrente real ni un navegador end-to-end completo.**

---

# 43. Checklist de salida

- [ ] formulo comportamiento antes de escribir test;
- [ ] escribo unit tests de Service con Mockito;
- [ ] explico `@Mock`, `@InjectMocks` y `MockitoExtension`;
- [ ] uso `when`, `verify` y `assertThrows` con intención;
- [ ] sé por qué Mockito no prueba JPA;
- [ ] explico `@SpringBootTest`;
- [ ] distingo `contextLoads()` de un flujo integrado;
- [ ] pruebo Services/JPA/DB;
- [ ] uso `MockMvc` con HTTP Basic;
- [ ] compruebo status, headers y JSON;
- [ ] explico qué cubre `MockMvc` y qué no;
- [ ] distingo lógica de infraestructura;
- [ ] recorro `BookShelf` de identidad a persistencia;
- [ ] diagnostico 400/401/404/409 por capas;
- [ ] describo cobertura sin exagerarla;
- [ ] no reintroduzco `borrowerId` desde el cliente en el flujo autenticado.

---

## Continúa con

**[Bloque 07 — Referencia →](../07-referencia/README.md)**

---

[← Índice de Calidad](README.md) · [Índice general](../README.md) · [Siguiente bloque → Referencia](../07-referencia/README.md)
