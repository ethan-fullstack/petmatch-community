# Práctica de transferencia — autenticación y autorización en BookShelf

Esta práctica acompaña los capítulos 22–25 y continúa `BookShelf` después de la práctica Web MVC.

Hasta este punto ya tienes:

```text
Spring MVC
→ Thymeleaf
→ Form DTO
→ Bean Validation
→ Service
→ JPA/MySQL
```

Ahora necesitamos responder:

> **¿Quién está usando la aplicación y qué recursos puede modificar?**

Esta práctica reproduce los conceptos principales de autenticación, Spring Security, protección de contraseñas y ownership en un dominio distinto de PetMatch.

> [!IMPORTANT]
> Esta práctica se detiene en el capítulo 25. No entra todavía en la explicación profunda de CSRF, sesión web ni seguridad REST stateless del capítulo 26.

---

## Qué vas a construir

Agregarás:

```text
registro de cuenta
→ contraseña almacenada como hash
→ login por email
→ rutas públicas y protegidas
→ rol USER / ADMIN
→ usuario actual
→ ownership sobre préstamos
```

El objetivo no es construir un sistema de seguridad completo, sino reconocer cómo se conectan:

```text
User Entity
→ UserRepository
→ UserService
→ UserDetailsService
→ PasswordEncoder
→ SecurityFilterChain
→ Authentication
→ Service ownership
```

---

# 1. Relación con los capítulos 22–25

| Capítulo | Concepto | Aplicación en BookShelf |
|---|---|---|
| 22 | autenticación | email como identidad, `UserDetailsService`, principal |
| 23 | Spring Security | `SecurityFilterChain`, rutas públicas/protegidas, form login |
| 24 | contraseñas | `PasswordEncoder`, hash, registro seguro |
| 25 | autorización y ownership | cada usuario ve y devuelve sus propios préstamos |

---

# 2. Dependencia de seguridad

En `pom.xml` agrega:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Después ejecuta:

```bash
./mvnw test
```

La presencia de Spring Security puede cambiar el comportamiento de rutas web incluso antes de tu configuración personalizada. Eso es una señal de que la infraestructura de seguridad ya está activa.

---

# 3. Define roles

Ruta sugerida:

```text
src/main/java/com/example/bookshelf/model/enums/Role.java
```

```java
package com.example.bookshelf.model.enums;

public enum Role {
    USER,
    ADMIN
}
```

No confundas rol con ownership.

```text
ROLE_USER
→ categoría general de permiso

loan.borrower
→ relación concreta entre usuario y recurso
```

Un usuario con rol USER no debería poder devolver cualquier préstamo USER.

---

# 4. Evoluciona `LibraryUser`

Amplía la Entity con:

```text
passwordHash
role
active
```

Ejemplo conceptual:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role = Role.USER;

@Column(nullable = false)
private boolean active = true;

@Column(nullable = false, length = 255)
private String passwordHash;
```

Ajusta el constructor para recibir el hash de contraseña.

No guardes:

```text
password
confirmPassword
```

como campos persistentes.

---

# 5. Crea `RegistrationForm`

Ruta:

```text
src/main/java/com/example/bookshelf/dto/auth/RegistrationForm.java
```

```java
package com.example.bookshelf.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    private String confirmPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
```

Otra vez:

```text
Form DTO
≠
Entity
```

---

# 6. Amplía `LibraryUserRepository`

```java
Optional<LibraryUser> findByEmailIgnoreCase(String email);

boolean existsByEmailIgnoreCase(String email);
```

Repository responde preguntas de persistencia.

No decide cómo se autentica el usuario.

---

# 7. Declara `PasswordEncoder`

Ruta:

```text
src/main/java/com/example/bookshelf/config/SecurityConfig.java
```

Comienza con:

```java
package com.example.bookshelf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    }
}
```

La idea clave:

```text
PasswordEncoder
→ Bean administrado por Spring
→ se inyecta en UserService
```

---

# 8. Hash no significa cifrado reversible

Cuando registras una contraseña:

```java
passwordEncoder.encode(rawPassword)
```

obtienes una representación derivada para verificación.

No deberías diseñar el sistema esperando:

```text
hash → recuperar contraseña original
```

La comparación se realiza mediante el mecanismo del encoder.

---

# 9. Crea una excepción de email duplicado

```java
package com.example.bookshelf.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already exists: " + email);
    }
}
```

---

# 10. Crea `LibraryUserService`

```java
package com.example.bookshelf.service;

import com.example.bookshelf.dto.auth.RegistrationForm;
import com.example.bookshelf.exception.DuplicateEmailException;
import com.example.bookshelf.model.LibraryUser;
import com.example.bookshelf.repository.LibraryUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class LibraryUserService {

    private final LibraryUserRepository libraryUserRepository;
    private final PasswordEncoder passwordEncoder;

    public LibraryUserService(
        LibraryUserRepository libraryUserRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.libraryUserRepository = libraryUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LibraryUser register(RegistrationForm form) {
        String email = normalizeEmail(form.getEmail());

        if (libraryUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        LibraryUser user = new LibraryUser(
            form.getName().trim(),
            email,
            passwordEncoder.encode(form.getPassword())
        );

        return libraryUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LibraryUser findByEmail(String email) {
        return libraryUserRepository
            .findByEmailIgnoreCase(normalizeEmail(email))
            .orElseThrow(() -> new UsernameNotFoundException(
                "Library user not found"
            ));
    }

    @Transactional(readOnly = true)
    public LibraryUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException(
                "No authenticated user"
            );
        }

        return findByEmail(authentication.getName());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
```

---

# 11. Registro y autenticación son procesos distintos

## Registro

```text
RegistrationForm
→ validar
→ normalizar email
→ comprobar duplicado
→ encode password
→ guardar LibraryUser
```

## Login

```text
email + password enviados
→ Spring Security
→ UserDetailsService
→ PasswordEncoder
→ Authentication
```

No implementes manualmente un POST `/login` que compare strings de contraseña.

---

# 12. Crea `DatabaseUserDetailsService`

Ruta:

```text
src/main/java/com/example/bookshelf/security/DatabaseUserDetailsService.java
```

```java
package com.example.bookshelf.security;

import com.example.bookshelf.model.LibraryUser;
import com.example.bookshelf.repository.LibraryUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService
    implements UserDetailsService {

    private final LibraryUserRepository libraryUserRepository;

    public DatabaseUserDetailsService(
        LibraryUserRepository libraryUserRepository
    ) {
        this.libraryUserRepository = libraryUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {

        LibraryUser user = libraryUserRepository
            .findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found"
            ));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .disabled(!user.isActive())
            .build();
    }
}
```

---

# 13. ¿Por qué el método se llama `loadUserByUsername` si usamos email?

El contrato de Spring Security usa el concepto genérico `username`.

En esta aplicación decidimos:

```text
username lógico
=
email
```

Por eso el método recibe un `String username`, pero lo consulta como email.

---

# 14. Entity `LibraryUser` y `UserDetails` no son lo mismo

```text
LibraryUser
→ modelo de dominio/persistencia

UserDetails
→ contrato que Spring Security entiende
```

`DatabaseUserDetailsService` funciona como adaptador entre ambos modelos.

---

# 15. Configura seguridad web

Amplía `SecurityConfig`:

```java
@Bean
SecurityFilterChain webSecurityFilterChain(HttpSecurity http)
    throws Exception {

    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                "/",
                "/login",
                "/register",
                "/error"
            ).permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .usernameParameter("email")
            .defaultSuccessUrl("/books", true)
            .permitAll()
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/?logout")
        );

    return http.build();
}
```

Para esta práctica basta una única chain web.

La API REST se estudiará después.

---

# 16. Lee las reglas de rutas

```text
/
/login
/register
/error
→ públicas

/admin/**
→ ADMIN

cualquier otra
→ usuario autenticado
```

Eso significa:

```text
usuario autenticado
→ puede llegar a /books
```

pero **no** significa:

```text
puede modificar cualquier recurso del sistema
```

La autorización por recurso sigue pendiente.

---

# 17. Crea `AuthController`

```java
package com.example.bookshelf.controller;

import com.example.bookshelf.dto.auth.RegistrationForm;
import com.example.bookshelf.exception.DuplicateEmailException;
import com.example.bookshelf.service.LibraryUserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final LibraryUserService libraryUserService;

    public AuthController(LibraryUserService libraryUserService) {
        this.libraryUserService = libraryUserService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute(
            "registrationForm",
            new RegistrationForm()
        );
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registrationForm") RegistrationForm form,
        BindingResult bindingResult
    ) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue(
                "confirmPassword",
                "password.mismatch",
                "Las contraseñas no coinciden."
            );
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            libraryUserService.register(form);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue(
                "email",
                "email.duplicate",
                "Ya existe una cuenta con este correo."
            );
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}
```

---

# 18. El POST `/login` no pertenece a `AuthController`

La vista puede enviar:

```html
<form th:action="@{/login}" method="post">
```

pero no necesitas escribir:

```java
@PostMapping("/login")
```

para comparar credenciales.

La `SecurityFilterChain` y Spring Security procesan ese flujo.

---

# 19. Crea `auth/login.html`

```html
<!doctype html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/head :: head('Iniciar sesión')}"></head>
<body>
    <main>
        <h1>Iniciar sesión</h1>

        <p th:if="${param.error}">
            Credenciales inválidas.
        </p>

        <p th:if="${param.registered}">
            Cuenta creada. Ya puedes iniciar sesión.
        </p>

        <form th:action="@{/login}" method="post">
            <label for="email">Correo</label>
            <input id="email" name="email" type="email">

            <label for="password">Contraseña</label>
            <input id="password" name="password" type="password">

            <button type="submit">Entrar</button>
        </form>
    </main>
</body>
</html>
```

El nombre:

```html
name="email"
```

coincide con:

```java
.usernameParameter("email")
```

---

# 20. Crea `auth/register.html`

Usa `th:object` y `th:field` como en la práctica MVC.

Ejemplo:

```html
<form
    th:action="@{/register}"
    th:object="${registrationForm}"
    method="post"
>
    <input th:field="*{name}" type="text">
    <p th:errors="*{name}"></p>

    <input th:field="*{email}" type="email">
    <p th:errors="*{email}"></p>

    <input th:field="*{password}" type="password">
    <p th:errors="*{password}"></p>

    <input th:field="*{confirmPassword}" type="password">
    <p th:errors="*{confirmPassword}"></p>

    <button type="submit">Crear cuenta</button>
</form>
```

---

# 21. Verifica el hash en base de datos

Registra un usuario.

Después consulta:

```sql
SELECT id, name, email, password_hash, role, active
FROM library_users;
```

Debes comprobar:

```text
password_hash
≠
contraseña original en texto plano
```

No imprimas contraseñas ni hashes en logs solo para demostración.

---

# 22. Verifica login

Prueba:

```text
GET /books sin autenticar
→ redirección a login
```

Después inicia sesión con una cuenta registrada.

Esperado:

```text
credenciales
→ UserDetailsService
→ usuario DB
→ PasswordEncoder
→ Authentication
→ /books
```

---

# 23. Accede al usuario actual desde Service

Ya tienes:

```java
libraryUserService.getCurrentUser(authentication)
```

Puedes utilizarlo en casos de uso donde el recurso depende de identidad.

No pases desde el formulario:

```text
borrowerId = cualquier usuario
```

si la intención real es:

```text
borrower = usuario autenticado
```

---

# 24. Evoluciona el flujo de préstamo

Antes la práctica de persistencia utilizaba:

```java
borrowBook(bookId, borrowerId, dueAt)
```

Ahora puedes evolucionarlo a:

```java
borrowBook(
    Long bookId,
    LocalDateTime dueAt,
    Authentication authentication
)
```

Y dentro:

```java
LibraryUser borrower =
    libraryUserService.getCurrentUser(authentication);
```

Eso evita que el cliente decida quién recibe el préstamo.

---

# 25. Ownership en `LoanRepository`

Agrega una consulta como:

```java
@EntityGraph(attributePaths = {
    "book",
    "book.category",
    "borrower"
})
Optional<Loan> findByIdAndBorrowerId(
    Long loanId,
    Long borrowerId
);
```

La pregunta ya no es:

```text
¿existe Loan 20?
```

sino:

```text
¿existe Loan 20 perteneciente al usuario actual?
```

---

# 26. Crea `findOwnedLoan(...)`

En `LoanService`:

```java
@Transactional(readOnly = true)
public Loan findOwnedLoan(
    Long loanId,
    Authentication authentication
) {
    LibraryUser borrower =
        libraryUserService.getCurrentUser(authentication);

    return loanRepository
        .findByIdAndBorrowerId(
            loanId,
            borrower.getId()
        )
        .orElseThrow(() -> new ResourceNotFoundException(
            "Loan not found: " + loanId
        ));
}
```

Esto combina:

```text
Authentication
→ current user
→ user.id
→ Repository ownership query
```

---

# 27. Protege devolución por ownership

Evoluciona:

```java
returnLoan(Long loanId)
```

hacia:

```java
@Transactional
public void returnLoan(
    Long loanId,
    Authentication authentication
) {
    Loan loan = findOwnedLoan(loanId, authentication);

    if (loan.getStatus() != LoanStatus.ACTIVE) {
        throw new LoanRuleException(
            "Only an ACTIVE loan can be returned."
        );
    }

    loan.markReturned(LocalDateTime.now());
    loan.getBook().markAvailable();
}
```

Ahora hay dos preguntas distintas:

```text
¿es mío?
→ autorización / ownership

¿está ACTIVE?
→ regla de estado
```

Ambas deben cumplirse.

---

# 28. Roles no reemplazan ownership

Supongamos:

```text
Ana → ROLE_USER
Luis → ROLE_USER
```

Si Ana tiene `Loan 10`, Luis sigue siendo USER.

Pero:

```text
ROLE_USER
≠
owner de Loan 10
```

Por eso una regla como:

```java
.anyRequest().authenticated()
```

no basta para proteger recursos individuales.

---

# 29. Crea una página “Mis préstamos”

Ruta:

```text
GET /loans
```

Controller:

```java
@GetMapping("/loans")
public String myLoans(
    Authentication authentication,
    Model model
) {
    model.addAttribute(
        "loans",
        loanService.findCurrentUserLoans(authentication)
    );
    return "loans/list";
}
```

Service:

```java
@Transactional(readOnly = true)
public List<Loan> findCurrentUserLoans(
    Authentication authentication
) {
    LibraryUser borrower =
        libraryUserService.getCurrentUser(authentication);

    return loanRepository
        .findByBorrowerIdOrderByBorrowedAtDesc(
            borrower.getId()
        );
}
```

La identidad ya no viene de un query parameter controlado por el navegador.

---

# 30. Ocultar un botón no es autorización

Puedes mostrar “Devolver” solo si:

```text
loan.status == ACTIVE
```

Pero aunque el botón no aparezca, alguien podría intentar invocar manualmente una URL.

La protección real sigue en:

```text
SecurityFilterChain
+
LoanService
+
ownership query
+
state rule
```

---

# 31. Agrega navegación según autenticación

Si agregas `thymeleaf-extras-springsecurity6`, puedes usar expresiones de seguridad en la vista.

Dependencia:

```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

En el fragment:

```html
<html
    lang="es"
    xmlns:th="http://www.thymeleaf.org"
    xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
>
```

Ejemplo:

```html
<div sec:authorize="isAuthenticated()">
    <span sec:authentication="name"></span>
</div>
```

Eso mejora la interfaz.

No reemplaza seguridad backend.

---

# 32. Ruta ADMIN de demostración

Puedes crear:

```text
GET /admin
```

Y protegerla con:

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
```

El objetivo es distinguir:

```text
rol
→ autorización general por categoría de usuario

ownership
→ autorización sobre un recurso concreto
```

No necesitas construir un panel administrativo completo.

---

# 33. Estado `active`

`DatabaseUserDetailsService` transforma:

```java
.disabled(!user.isActive())
```

Eso significa que un usuario persistente puede existir pero quedar deshabilitado para autenticación.

No confundas:

```text
usuario inexistente
```

con:

```text
usuario existente pero deshabilitado
```

---

# 34. Verificación 1 — registro inválido

Prueba:

```text
email inválido
password corta
confirmPassword distinta
```

Esperado:

```text
RegistrationForm
→ @Valid
→ BindingResult
→ auth/register
```

Si las contraseñas no coinciden, el Controller agrega un error al field correspondiente.

---

# 35. Verificación 2 — email duplicado

Registra dos veces el mismo correo con diferencias de mayúsculas/minúsculas.

El Service normaliza email y Repository consulta case-insensitive.

Esperado:

```text
DuplicateEmailException
→ Controller
→ error en email
```

---

# 36. Verificación 3 — login correcto e incorrecto

Prueba contraseña incorrecta.

Después la correcta.

Debes poder explicar el recorrido:

```text
form login
→ email
→ UserDetailsService
→ UserRepository
→ UserDetails
→ PasswordEncoder
→ Authentication
```

---

# 37. Verificación 4 — ruta protegida

Sin sesión autenticada:

```text
GET /books
```

no debería comportarse como una ruta pública.

Después de login sí debe ser accesible para USER.

---

# 38. Verificación 5 — ADMIN

Con una cuenta USER intenta acceder:

```text
/admin
```

Debe quedar bloqueada por la regla de rol.

Con una cuenta ADMIN debería poder superar esa decisión general.

---

# 39. Verificación 6 — ownership

Crea dos usuarios:

```text
Ana
Luis
```

Crea un préstamo para Ana.

Intenta ejecutar la devolución como Luis usando el `loanId` de Ana.

Esperado:

```text
Luis autenticado
→ getCurrentUser
→ findByIdAndBorrowerId(loanId, luisId)
→ no encontrado
→ operación rechazada
```

Aunque Luis conozca el identificador.

---

# 40. Autenticación ≠ autorización

```text
Autenticación
→ ¿quién eres?

Autorización
→ ¿puedes realizar esta operación?
```

Un usuario puede estar correctamente autenticado y no estar autorizado para un recurso concreto.

---

# 41. Rol ≠ ownership

```text
hasRole("ADMIN")
→ política por rol
```

```text
findByIdAndBorrowerId(...)
→ política por relación con recurso
```

Ambas son formas de autorización, pero responden preguntas distintas.

---

# 42. PasswordEncoder pertenece a infraestructura de seguridad

No conviertas la Entity en responsable de:

```java
passwordEncoder.encode(...)
```

La Entity representa datos/estado del dominio.

El Service coordina registro y recibe el `PasswordEncoder` como dependencia.

---

# 43. No compares hashes manualmente

Evita diseñar:

```java
if (storedHash.equals(passwordEncoder.encode(rawPassword)))
```

Muchos algoritmos usan sal aleatoria; dos llamadas a `encode` no tienen por qué producir el mismo string.

Deja que Spring Security y el `PasswordEncoder` realicen la verificación adecuada.

---

# 44. No uses contraseña en texto plano “solo para aprender”

El objetivo de una práctica pedagógica no justifica enseñar un patrón inseguro como paso normal.

Desde el primer registro persistente:

```text
raw password
→ encode
→ passwordHash
```

---

# 45. No metas ownership en Thymeleaf

Evita una lógica como:

```text
si el email de pantalla coincide, entonces permitir operación
```

La vista puede decidir qué mostrar.

La autorización debe seguir protegida cuando la request llega al backend.

---

# 46. No pongas toda la autorización en la filter chain

Una filter chain puede decidir:

```text
/books/** requiere autenticación
/admin/** requiere ADMIN
```

Pero no conoce naturalmente cada relación persistente como:

```text
Loan 14 pertenece a Ana
```

Ese nivel se resuelve en Service/Repository según el dominio.

---

# 47. Checkpoint 22 — Autenticación

Explica:

```text
¿qué dato funciona como username lógico?
¿quién carga el usuario desde DB?
¿qué convierte LibraryUser en UserDetails?
¿qué representa Authentication?
```

Revisa [capítulo 22](22-autenticacion.md).

---

# 48. Checkpoint 23 — Spring Security

Explica esta configuración:

```text
permitAll
hasRole
anyRequest.authenticated
formLogin
usernameParameter("email")
```

Y responde:

```text
¿quién procesa POST /login?
```

Revisa [capítulo 23](23-spring-security.md).

---

# 49. Checkpoint 24 — Contraseñas

Explica:

```text
raw password
→ PasswordEncoder
→ passwordHash
```

Y por qué:

```text
hash
≠
cifrado reversible
```

Revisa [capítulo 24](24-contrasenas-y-password-encoder.md).

---

# 50. Checkpoint 25 — Ownership

Clasifica:

```text
/admin/** solo ADMIN
→ rol

Loan pertenece a borrower actual
→ ownership

Loan debe estar ACTIVE para devolver
→ regla de estado

/books requiere login
→ autenticación requerida por ruta
```

Revisa [capítulo 25](25-autorizacion-y-ownership.md).

---

# 51. Prueba de transferencia final

Compara patrones:

| Concepto | PetMatch | BookShelf |
|---|---|---|
| identidad de login | email | email |
| Entity persistente | `User` | `LibraryUser` |
| adaptador Security | `DatabaseUserDetailsService` | `DatabaseUserDetailsService` |
| hash | `PasswordEncoder` | `PasswordEncoder` |
| recurso con ownership | `Pet`, `SupportRequest` | `Loan` |
| query ownership | `findByIdAndOwnerId` | `findByIdAndBorrowerId` |
| rol general | USER/ADMIN | USER/ADMIN |

La finalidad no es copiar nombres, sino reconocer el patrón de seguridad.

---

# 52. Checklist de salida

Antes de continuar con el capítulo 26 o REST, comprueba que puedes:

- [ ] distinguir autenticación de autorización;
- [ ] almacenar password hash y no texto plano;
- [ ] declarar un `PasswordEncoder` Bean;
- [ ] inyectar `PasswordEncoder` en Service;
- [ ] registrar un usuario desde Form DTO;
- [ ] normalizar email;
- [ ] detectar email duplicado;
- [ ] implementar `UserDetailsService`;
- [ ] adaptar Entity → `UserDetails`;
- [ ] explicar `Authentication`;
- [ ] configurar rutas públicas y autenticadas;
- [ ] configurar `hasRole("ADMIN")`;
- [ ] usar `usernameParameter("email")`;
- [ ] explicar por qué Spring Security procesa el login;
- [ ] obtener el usuario actual en Service;
- [ ] evitar recibir ownership desde un campo manipulable del formulario;
- [ ] crear una query de ownership;
- [ ] proteger una operación por ownership;
- [ ] distinguir rol, ownership y regla de estado;
- [ ] explicar por qué ocultar botones no protege el endpoint.

---

## Resultado esperado

Al terminar deberías poder explicar una operación protegida como:

```text
usuario inicia sesión
→ Spring Security crea Authentication
→ Controller recibe Authentication
→ Service obtiene LibraryUser actual
→ Repository consulta recurso + owner
→ ownership válido
→ regla de estado válida
→ operación ejecutada
```

Y también distinguir dónde participa el rol:

```text
/admin/**
→ decisión general de SecurityFilterChain
```

frente a:

```text
Loan 14
→ decisión específica del dominio
```

---

## Continúa con

El siguiente capítulo completa la seguridad web con CSRF y sesión:

**[Capítulo 26 — CSRF, sesión y seguridad web →](26-csrf-sesion-y-seguridad-web.md)**

---

[← Índice Seguridad](README.md) · [Índice general](../README.md) · [Siguiente → Capítulo 26](26-csrf-sesion-y-seguridad-web.md)
