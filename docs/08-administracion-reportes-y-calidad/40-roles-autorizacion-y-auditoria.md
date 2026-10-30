# 40 — Administración de roles, autorización y auditoría

En el [capítulo 25](../04-seguridad/25-autorizacion-y-ownership.md) vimos cómo impedir que una persona edite mascotas o solicitudes que pertenecen a otra. Pero una aplicación que crece necesita resolver otro problema:

> **¿Cómo puede una cuenta autorizada administrar los permisos de otras personas sin permitir el autoescalamiento, sin conservar privilegios revocados y sin perder al último administrador?**

Este capítulo utiliza **PetMatch Community** como caso de estudio y busca que puedas aplicar las ideas a una aplicación de préstamos, inventarios, reservas o servicios. No se trata de añadir roles por cumplir una lista: la administración aparece cuando existe una necesidad funcional real.

Al finalizar podrás distinguir autenticación, roles, *ownership* y actividad de la cuenta; seguir una operación administrativa desde HTTP hasta JPA; explicar por qué las sesiones antiguas requieren una política explícita; identificar la regla de conservación del último administrador; interpretar un registro de auditoría y diseñar pruebas positivas, negativas y concurrentes.

---

## 1. El problema: una identidad no tiene todos los permisos

En un sistema de préstamos, una persona puede registrar sus propios préstamos, mientras que otra administra permisos o reportes globales. Son responsabilidades distintas.

| Pregunta | Concepto | Ejemplo |
|---|---|---|
| ¿Quién inició sesión? | Autenticación | «La sesión pertenece a Ana». |
| ¿Puede realizar una operación administrativa? | Autorización por rol | «Ana tiene permiso para cambiar roles». |
| ¿Puede modificar este recurso concreto? | *Ownership* | «La solicitud pertenece a Bruno». |
| ¿La cuenta todavía puede utilizar el sistema? | Estado de cuenta | «La cuenta fue desactivada». |
| ¿Quién ejecutó un cambio importante? | Auditoría | «Ana cambió el rol de Bruno». |

Un administrador no adquiere automáticamente la propiedad de todos los recursos. Son políticas complementarias, no equivalentes.

**🧠 Idea mental**

```text
Identidad autenticada
         ↓
Cuenta habilitada
         ↓
Permiso para la operación
         ↓
Regla del dominio
         ↓
Cambio y registro correspondiente
```

**Antes de continuar:** identifica una acción de tu aplicación que deba estar disponible para cualquier usuario autenticado y otra que realmente requiera privilegios administrativos. Si no encuentras la segunda, no necesitas inventar un panel de administración.

## 2. Un modelo de roles deliberadamente pequeño

**Código real — [Role.java](../../src/main/java/com/petmatch/community/model/enums/Role.java):**

```java
public enum Role {
    USER,
    ADMIN
}
```

La entidad [User.java](../../src/main/java/com/petmatch/community/model/User.java) contiene:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Role role;

@Column(nullable = false)
private boolean active;
```

`@Enumerated(EnumType.STRING)` persiste el nombre del enum y evita asociar el significado del rol a una posición numérica. `active` representa si la cuenta puede continuar utilizando las funcionalidades protegidas.

Al crear una cuenta, `User.initializeDefaults()` asigna `USER` cuando no se especifica rol y establece la cuenta como activa. [UserService.register(...)](../../src/main/java/com/petmatch/community/service/UserService.java) construye un usuario normal, sin aceptar desde el registro público una decisión sobre sus privilegios.

**Consecuencia:** agregar manualmente `role=ADMIN` a una petición de registro no concede ese rol. La prueba `publicRegistrationCannotEscalateRole()` de [AdminIntegrationTests](../../src/test/java/com/petmatch/community/integration/AdminIntegrationTests.java) comprueba precisamente esa frontera.

**Decisión:** dos roles son suficientes para el alcance actual. Si una organización realmente necesitara permisos configurables por módulo, podría evaluarse un modelo de permisos separado; no es necesario introducirlo de forma anticipada.

## 3. Rol persistido y autoridad de Spring Security

En la base, el rol de un usuario es `ADMIN`. Sin embargo, Spring Security representa habitualmente un rol como una **autoridad** con prefijo `ROLE_`.

**Código real — [DatabaseUserDetailsService](../../src/main/java/com/petmatch/community/security/DatabaseUserDetailsService.java):**

```java
return org.springframework.security.core.userdetails.User
    .withUsername(user.getEmail())
    .password(user.getPasswordHash())
    .roles(user.getRole().name())
    .disabled(!user.isActive())
    .build();
```

El resultado conceptual de `.roles("ADMIN")` es una autoridad `ROLE_ADMIN`.

```text
User.role = ADMIN            Dato del dominio
        ↓
.roles("ADMIN")              Adaptación de Spring Security
        ↓
ROLE_ADMIN                   Autoridad de la autenticación
```

No confundas esos valores:

- `Role` pertenece al modelo persistido.
- `Authentication` representa la identidad y las autoridades de una autenticación.
- Una sesión web puede conservar una autenticación creada **antes** de que alguien cambiara el rol persistido.

Este último punto es importante: cambiar un campo en la base **no actualiza automáticamente las autoridades de una sesión que ya estaba abierta**.

Para repasar el mecanismo general, consulta [Autenticación](../04-seguridad/22-autenticacion.md) y [Spring Security](../04-seguridad/23-spring-security.md).

## 4. Ocultar un enlace no protege un endpoint

La navegación de PetMatch solo muestra la sección administrativa cuando la sesión incluye el rol adecuado.

**Código real — [navigation.html](../../src/main/resources/templates/fragments/navigation.html):**

```html
<div class="pm-nav__admin" sec:authorize="hasRole('ADMIN')">
    <span class="pm-nav__admin-label">Administración</span>
    <a th:href="@{/admin/users}" class="pm-nav__link pm-nav__link--admin">Usuarios</a>
    <a th:href="@{/admin/reports}" class="pm-nav__link pm-nav__link--admin">Reportes</a>
</div>
```

`sec:authorize` decide qué se renderiza en HTML. Pero un cliente puede construir una petición HTTP sin usar ese menú:

```http
POST /admin/users/42/role
Content-Type: application/x-www-form-urlencoded

role=ADMIN
```

Por eso la autorización **no** debe depender de Thymeleaf ni de JavaScript.

**⚠️ Error frecuente:** pensar que una operación es segura porque el usuario no encuentra el botón. La decisión importante debe tomarse en el servidor.

## 5. La primera frontera: seguridad HTTP

[SecurityConfig.java](../../src/main/java/com/petmatch/community/config/SecurityConfig.java) configura una cadena para la API `/api/**` y otra para las páginas MVC.

En la cadena web aparece:

**Código real — fragmento:**

```java
.authorizeHttpRequests(authorize -> authorize
    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
    .requestMatchers("/", "/login", "/register", "/error", "/css/**", "/js/**").permitAll()
    .requestMatchers("/admin/**").access(currentAdminAuthorization)
    .anyRequest().access(currentUserAuthorization)
)
```

El orden es relevante. Las rutas administrativas pasan por una política **más específica**; las demás rutas web protegidas requieren una cuenta activa mediante otra política.

Estas clases implementan el contrato `AuthorizationManager<RequestAuthorizationContext>` de Spring Security:

- [CurrentAdminAuthorization](../../src/main/java/com/petmatch/community/security/CurrentAdminAuthorization.java).
- [CurrentUserAuthorization](../../src/main/java/com/petmatch/community/security/CurrentUserAuthorization.java).

La política HTTP responde si una petición puede continuar. Las reglas de *ownership* siguen siendo responsabilidad de los casos de uso. Lee [Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md) si esa diferencia aún no es clara.

## 6. Revocar privilegios cuando existe una sesión antigua

Imagina:

1. Ana inicia sesión como administradora.
2. Spring Security guarda `ROLE_ADMIN` en la autenticación de esa sesión.
3. Otra cuenta autorizada cambia el rol de Ana a `USER`.
4. Ana conserva la sesión anterior e intenta acceder a `/admin/users`.

Si el sistema solo consultara la autoridad guardada en la sesión, podría seguir aceptando un privilegio que ya fue revocado.

PetMatch consulta la autenticación **y** los datos actuales de la cuenta.

**Código real — parte de `CurrentAdminAuthorization.authorize(...)`:**

```java
Authentication current = authentication.get();

if (current == null || !current.isAuthenticated()
        || current instanceof AnonymousAuthenticationToken
        || current.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
    return new AuthorizationDecision(false);
}

boolean permitted = users.findByEmailIgnoreCase(current.getName())
        .map(user -> user.isActive() && user.getRole() == Role.ADMIN)
        .orElse(false);

return new AuthorizationDecision(permitted);
```

La primera comprobación exige `ROLE_ADMIN` en la autenticación. La segunda exige `Role.ADMIN` y `active=true` en la cuenta persistida.

| Sesión actual | Rol persistido | Activo | Acceso administrativo |
|---|---|---|---|
| `ROLE_USER` | `USER` | Sí | Denegado |
| `ROLE_USER` | `ADMIN` | Sí | Denegado hasta un nuevo inicio de sesión |
| `ROLE_ADMIN` | `ADMIN` | Sí | Permitido |
| `ROLE_ADMIN` | `USER` | Sí | Denegado: el permiso fue revocado |
| `ROLE_ADMIN` | `ADMIN` | No | Denegado |
| Sin autenticación | — | — | Denegado |

**🧠 Regla para recordar:** una promoción **no concede** privilegios a una sesión antigua; una degradación **sí impide** seguir utilizando privilegios que ya no corresponden.

Este control se aplica cuando llega una nueva petición protegida. No significa que una página previamente cargada desaparezca de inmediato del navegador ni constituye por sí solo un sistema distribuido de invalidación de sesiones.

### Cuentas desactivadas en las rutas ordinarias

[CurrentUserAuthorization](../../src/main/java/com/petmatch/community/security/CurrentUserAuthorization.java) también vuelve a consultar el campo `active` para las rutas web no administrativas. Además, [UserService.getCurrentUser(...)](../../src/main/java/com/petmatch/community/service/UserService.java) no devuelve una cuenta inactiva a los Services que lo utilizan.

La prueba `deactivatedAccountCannotContinueUsingExistingWebSession()` en [WebRegressionTests](../../src/test/java/com/petmatch/community/integration/WebRegressionTests.java) cubre ese caso.

**Límite actual:** el panel muestra si la cuenta está activa, pero **no ofrece un formulario para activar o desactivar usuarios**. El estado se utiliza en seguridad, sin constituir una función administrativa de edición del estado.

## 7. El controlador recibe los datos, no inventa permisos

El punto de entrada está en [AdminUserController.java](../../src/main/java/com/petmatch/community/controller/AdminUserController.java).

**Código real — métodos del controlador:**

```java
@GetMapping
public String list(@RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute("users", service.list(page));
    model.addAttribute("roles", Role.values());
    return "admin/users";
}

@PostMapping("/{id}/role")
public String change(
        @PathVariable Long id,
        @RequestParam Role role,
        Principal actor,
        RedirectAttributes flash) {
    try {
        service.changeRole(actor.getName(), id, role);
        flash.addFlashAttribute(
                "successMessage",
                "Rol actualizado. El usuario debe iniciar sesión nuevamente.");
    } catch (IllegalArgumentException e) {
        flash.addFlashAttribute("errorMessage", e.getMessage());
    }
    return "redirect:/admin/users";
}
```

Observa las responsabilidades:

- `GET /admin/users` solicita una lista paginada.
- `POST /admin/users/{id}/role` solicita una modificación.
- `Principal` proporciona la identidad autenticada del actor. Su correo **no** proviene de un campo editable llamado `actorEmail`.
- El Controller **delega** la validación de la transición al Service.
- La vista recibe mensajes de resultado mediante atributos *flash*.

El parámetro `role` se convierte al enum `Role`. Un valor inexistente, como `SUPERUSER`, o la ausencia del parámetro requerido produce un rechazo de entrada; esos casos están cubiertos en `AdminIntegrationTests`.

### ¿Por qué una petición POST?

Cambiar un rol altera el estado del sistema; no debe ejecutarse mediante un enlace `GET`. La cadena de seguridad web mantiene la protección **CSRF** para métodos como `POST`.

En Thymeleaf, la integración de formularios con Spring Security puede incorporar el token CSRF correspondiente. `AdminIntegrationTests.adminRequiresCsrf()` confirma que el cambio de rol es rechazado cuando falta el token.

Para profundizar, revisa [CSRF, sesión y seguridad web](../04-seguridad/26-csrf-sesion-y-seguridad-web.md).

## 8. La operación administrativa real está en el Service

Una petición autorizada todavía puede intentar ejecutar una modificación incorrecta. Por eso [AdminUserService.changeRole(...)](../../src/main/java/com/petmatch/community/service/AdminUserService.java) conserva las reglas de negocio dentro de una transacción.

Su secuencia de decisiones puede resumirse así:

```text
Validar el ID y el rol solicitados
        ↓
Obtener y bloquear las cuentas
        ↓
Comprobar que el actor sigue siendo ADMIN y está activo
        ↓
Encontrar la cuenta objetivo
        ↓
Si el rol no cambia, finalizar sin auditoría
        ↓
Impedir que desaparezca el último administrador activo
        ↓
Guardar RoleChange y actualizar User.role
```

### 8.1 Validar entradas

**Código real:**

```java
if (targetId == null || targetId < 1 || role == null)
    throw new IllegalArgumentException("Usuario o rol inválido");
```

La validación protege el contrato del Service, incluso si otro adaptador lo invocara sin pasar por la vista actual.

### 8.2 Volver a comprobar la autorización

El Service identifica al actor dentro de las cuentas consultadas y verifica de nuevo que esté activo y mantenga `Role.ADMIN`. No supone que el rol del usuario permanezca igual desde el instante en que llegó la petición HTTP.

Esto es **defensa en profundidad**: la filter chain autoriza la entrada y la capa de negocio protege la operación.

### 8.3 Evitar cambios sin efecto

**Código real:**

```java
if (target.getRole() == role) return;
```

Guardar el mismo rol no constituye una transición. En PetMatch tampoco se crea un nuevo registro de auditoría para ese caso.

### 8.4 La invariante del último administrador

PetMatch establece una regla:

> **Debe existir al menos un administrador activo.**

**Código real — condición esencial:**

```java
if (target.isActive()
        && target.getRole() == Role.ADMIN
        && role != Role.ADMIN
        && locked.stream()
                .filter(u -> u.isActive() && u.getRole() == Role.ADMIN)
                .count() <= 1)
    throw new IllegalArgumentException(
            "No se puede degradar al último administrador activo");
```

La regla no proviene de Spring Security. Es una necesidad del negocio administrativo.

**Antes de ejecutar:** si existen dos administradores activos, ¿puede degradarse uno? ¿Y si luego se intenta degradar al otro? Justifica qué condición cambia.

### 8.5 Cambio y auditoría

**Código real:**

```java
changes.save(new RoleChange(actor.getId(), targetId, target.getRole(), role));
target.setRole(role);
```

El cambio y su registro pertenecen a la misma operación `@Transactional`. Como `target` es una entidad gestionada por JPA, el nuevo rol puede persistirse mediante *dirty checking* sin llamar de nuevo a `users.save(target)`.

Esta relación entre transacción y entidades gestionadas se estudió en [Transacciones y consistencia](../02-dominio-y-persistencia/14-transacciones-y-consistencia.md).

## 9. Dos operaciones concurrentes pueden romper una regla

Supongamos que solo existen dos administradores, Ana y Bruno. Sin coordinación entre transacciones:

```text
Operación A: observa 2 administradores activos.
Operación B: observa 2 administradores activos.

Operación A: degrada a Ana.
Operación B: degrada a Bruno.

Estado final incorrecto: ningún administrador activo.
```

Cada operación pudo evaluar una condición que parecía válida, pero ambas juntas rompieron la invariante.

### ¿Qué hace PetMatch?

[UserRepository.lockForRoleChange()](../../src/main/java/com/petmatch/community/repository/UserRepository.java) utiliza un bloqueo pesimista:

**Código real:**

```java
@org.springframework.data.jpa.repository.Lock(
        jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
@org.springframework.data.jpa.repository.Query("select u from User u order by u.id")
java.util.List<User> lockForRoleChange();
```

`AdminUserService.changeRole()` utiliza `@Transactional`. Con una base de datos compatible, el bloqueo coordina las transacciones que intentan cambiar roles mediante este caso de uso. Las operaciones vuelven a comprobar las reglas antes de persistir su decisión.

**🧪 Comprobación real:** [RoleConcurrencyTests.simultaneousSelfDemotionsLeaveOneActiveAdmin()](../../src/test/java/com/petmatch/community/integration/RoleConcurrencyTests.java) ejecuta dos degradaciones concurrentes. Se espera que solo una pueda realizarse y que permanezca un administrador activo.

### El costo de esta decisión

La consulta bloquea **todas las filas de usuarios seleccionadas**. Para una aplicación demostrativa pequeña puede ser sencillo de explicar y probar; para un producto con muchas cuentas, puede aumentar las esperas y reducir el rendimiento.

En una aplicación de mayor escala habría que evaluar otras estrategias de coordinación y medirlas. El uso de `PESSIMISTIC_WRITE` no es automáticamente la opción óptima para cualquier escenario.

Tampoco debe suponerse que este mecanismo protege frente a modificaciones arbitrarias realizadas por fuera del caso de uso, como escrituras directas no coordinadas en la base.

## 10. ¿Qué registra realmente la auditoría?

La entidad [RoleChange.java](../../src/main/java/com/petmatch/community/model/RoleChange.java) conserva un evento de cambio de rol.

| Campo | Significado |
|---|---|
| `actorId` | Identificador de la cuenta que ejecutó la operación |
| `targetId` | Identificador de la cuenta cuyo rol se modificó |
| `previousRole` | Rol anterior |
| `newRole` | Rol posterior |
| `changedAt` | Momento de creación del evento |

**Código real — constructor:**

```java
public RoleChange(Long actorId, Long targetId, Role previousRole, Role newRole) {
    this.actorId = actorId;
    this.targetId = targetId;
    this.previousRole = previousRole;
    this.newRole = newRole;
    this.changedAt = LocalDateTime.now();
}
```

Una auditoría útil responde preguntas sobre acciones importantes: **quién**, **sobre quién**, **qué cambió** y **cuándo**.

Pero la implementación actual **no** es un sistema general de auditoría: no registra todos los accesos, intentos fallidos, IP, dispositivos, justificaciones ni las operaciones que se hicieron directamente en SQL. Tampoco incorpora una pantalla para consultar el historial o mecanismos especiales que impidan cualquier modificación posterior de las filas.

El cambio de rol y el guardado del evento están dentro de la **misma transacción**. Si esa transacción se revierte, ambos efectos se revierten bajo las garantías de la base.

**⚠️ Error frecuente:** presentar una tabla `role_changes` como prueba de cumplimiento integral de normas de auditoría o trazabilidad. Es un historial específico de un evento de negocio.

## 11. La interfaz muestra datos mínimos y mantiene la autorización fuera

[admin/users.html](../../src/main/resources/templates/admin/users.html) muestra nombre, correo, rol, estado, selector de rol y paginación.

El Service usa un objeto de lectura para esa pantalla:

**Código real:**

```java
public record UserRow(Long id, String name, String email, Role role, boolean active) {}
```

No necesita entregar el hash de contraseña para construir la tabla.

La lista utiliza páginas de **20 usuarios**, ordenadas por ID. Cada fila tiene un formulario que envía el ID objetivo y el nuevo `role`. La etiqueta del selector y el texto accesible del botón permiten diferenciar operaciones entre varias filas.

**¿Por qué no validar la regla del último administrador directamente en JavaScript?** Porque un usuario podría omitir ese código, deshabilitarlo o enviar HTTP manualmente. La interfaz puede orientar, pero el Service sigue siendo responsable de la invariante.

La primera cuenta administradora se habilita mediante un **procedimiento controlado del operador**, no mediante el registro público o credenciales compartidas. Una promoción manual inicial realizada directamente en la base **no generará por sí misma** un `RoleChange` de JPA.

## 12. Pruebas que demuestran comportamientos distintos

No basta con ejecutar una operación de forma exitosa. Una política de seguridad necesita pruebas que intenten romper sus límites.

| Situación | Resultado esperado | Prueba existente |
|---|---|---|
| Visitante abre `/admin/users` | Redirección al login | `anonymousCannotReadAdminPages` |
| Cuenta normal consulta o cambia roles | Acceso denegado | `userCannotReadOrChangeThirdPartyRoles` |
| Administrador hace POST sin CSRF | Acceso denegado | `adminRequiresCsrf` |
| Registro público envía `role=ADMIN` | Persiste como `USER` | `publicRegistrationCannotEscalateRole` |
| Administrador cambia un rol válido | Cambio persistido y registrado | `promotionPersistsAndAuditsThenCanBeRevoked` |
| Intento de degradar al último administrador | Operación rechazada | `lastAdminCannotBeDemoted` |
| Cambio solicitado no modifica el rol | No crea un evento nuevo | `noOpDoesNotCreateAudit` |
| Promoción con sesión antigua | Necesita nuevo login | `rolePromotionNeedsFreshLoginAndDemotionRevokesExistingAdminSession` |
| Degradación con sesión antigua | Pierde acceso administrativo | Mismo test anterior |
| Cuenta desactivada conserva sesión | Pierde acceso web protegido | `deactivatedAccountCannotContinueUsingExistingWebSession` |
| Dos degradaciones simultáneas | Queda un administrador activo | `simultaneousSelfDemotionsLeaveOneActiveAdmin` |

**🔎 Archivos del repositorio:**

- [AdminIntegrationTests.java](../../src/test/java/com/petmatch/community/integration/AdminIntegrationTests.java).
- [WebRegressionTests.java](../../src/test/java/com/petmatch/community/integration/WebRegressionTests.java).
- [RoleConcurrencyTests.java](../../src/test/java/com/petmatch/community/integration/RoleConcurrencyTests.java).

Las pruebas HTTP usan MockMvc; la prueba concurrente requiere las transacciones y el motor de base de datos del entorno de tests.

### Ejecutar sin comprometer datos reales

La suite de PetMatch utiliza una base de datos de pruebas aislada configurada con `TEST_DB_URL`, `TEST_DB_USERNAME` y `TEST_DB_PASSWORD`. **No apuntes esas variables a una base que contenga datos importantes o de producción.**

Con el entorno de prueba preparado, desde la raíz del repositorio se ejecuta:

```bash
./mvnw clean verify
```

Una suite aprobada demuestra el cumplimiento de los casos que realmente ejecutó, en esa versión y ambiente. No garantiza ausencia total de vulnerabilidades.

## 13. Decisiones y alternativas: ¿qué conviene llevar a otra aplicación?

| Decisión en PetMatch | Beneficio | Costo o límite |
|---|---|---|
| Dos roles mediante enum | Modelo sencillo y explícito | Cambiar roles disponibles exige cambiar código |
| Comprobar rol persistido en cada petición administrativa | Revocación efectiva frente a sesiones desactualizadas | Consulta adicional por petición |
| Exigir nuevo login para conceder privilegios recién otorgados | Evita elevación silenciosa en sesiones antiguas | El usuario debe autenticarse nuevamente |
| Bloquear usuarios al cambiar rol | Protege la regla del último administrador ante concurrencia | Bloqueo amplio, potencialmente costoso |
| Guardar `RoleChange` en la transacción | Deja rastro del cambio confirmado | No equivale a auditoría general e inmutable |

No se necesita crear un patrón GoF específico para asignar roles. Las abstracciones de Spring Security y de JPA ya proporcionan los mecanismos necesarios; nuestra responsabilidad es definir bien las políticas y reglas.

**Qué no implementa esta aplicación:** una interfaz para activar/desactivar cuentas, un catálogo de permisos configurables, ACL, una pantalla de consulta del historial de roles o invalidación instantánea de todas las sesiones distribuidas. No debes describir esas capacidades como existentes.

## 14. ⚠️ Errores frecuentes

**Error A — Confiar solo en la interfaz.** Ocultar «Administración» no impide enviar peticiones HTTP. Protege los endpoints.

**Error B — Permitir que un campo del registro defina el rol.** El rol del nuevo usuario lo establece la política del servidor, no un valor enviado libremente por quien se registra.

**Error C — Confundir sesión con estado actual.** Una autenticación antigua puede contener autoridades que ya no corresponden a la base. Define cuándo se vuelve a comprobar la autorización.

**Error D — Comprobar el último administrador sin coordinar escrituras.** Dos operaciones concurrentes pueden ver el mismo recuento inicial. Prueba el escenario concurrente.

**Error E — Confundir auditabilidad parcial con auditoría total.** Registra los hechos que necesitas, indica los límites y conserva evidencia verificable.

**Error F — Dar propiedad total a todos los administradores.** La autorización global y el *ownership* son responsabilidades distintas.

## 15. 🔁 Llévalo a tu proyecto

Trabaja con tu propio dominio: préstamos de equipos, reservas, biblioteca, inventario u otro problema elegido por tu equipo. No copies las entidades de PetMatch.

**Paso 1 — Necesidad real.** Explica qué operación administrativa necesitan los usuarios y por qué no puede realizarla cualquier cuenta.

**Paso 2 — Matriz de permisos.** Completa:

| Actor | Operación | ¿Autorizada? | Condición adicional |
|---|---|---|---|
| Visitante | Consultar sección protegida | Por definir | ¿Debe autenticarse? |
| Usuario normal | Modificar un recurso ajeno | Por definir | ¿Existe *ownership*? |
| Administrador | Cambiar permisos | Por definir | ¿Qué regla protege la operación? |
| Cuenta desactivada | Continuar con sesión antigua | Por definir | ¿Cómo se revoca? |

**Paso 3 — Mecanismo mínimo.** Decide si necesitas roles fijos, permisos específicos o ninguna función administrativa adicional. Justifica la decisión.

**Paso 4 — Flujo de responsabilidades.** Dibuja: `HTTP → SecurityFilterChain → Controller → Service → Repository`, señalando dónde se obtiene la identidad del actor, dónde se conserva la invariante y dónde se registra el cambio.

**Paso 5 — Dos pruebas.** Diseña un caso positivo y otro en que una cuenta no autorizada intenta cambiar permisos. La prueba negativa debe verificar que los datos no cambiaron.

**Paso 6 — Concurrencia.** Si existe una regla que puede romperse con dos operaciones simultáneas, explica cómo la comprobarías. Si no existe ese riesgo, no agregues bloqueos artificiales.

**Paso 7 — Auditoría.** Define actor, destinatario, transición y momento. No recopiles información adicional sin necesidad.

La evidencia puede ser una explicación técnica breve con matriz y pruebas sobre tu aplicación. No necesitas producir un PDF ni agregar funcionalidades ficticias para demostrar comprensión.

## 16. 🧪 Comprueba que entendiste

1. ¿Por qué autenticación no equivale a autorización administrativa?
2. ¿Qué diferencia hay entre `Role.ADMIN` y `ROLE_ADMIN`?
3. Si una cuenta fue promovida en la base, ¿por qué una sesión antigua puede seguir sin entrar al panel?
4. Si fue degradada, ¿qué comprobación impide usar su antigua autoridad?
5. ¿Qué papel cumple `CurrentUserAuthorization` para cuentas inactivas?
6. ¿Por qué cambiar un rol usa POST con CSRF y no un enlace GET?
7. ¿Por qué el actor procede del `Principal` y no de un campo del formulario?
8. ¿Qué problema evita la invariante del último administrador?
9. ¿Por qué una prueba secuencial no sustituye una prueba concurrente?
10. ¿Qué registra `RoleChange` y qué información no registra?
11. ¿Qué costo tiene bloquear todas las cuentas en cada cambio de rol?
12. ¿Cuándo elegirías una lista de permisos configurable en lugar de dos valores de enum?

**Diagnóstico oral.** Decide qué sucede en cada caso:

```text
A: ROLE_ADMIN en la sesión, USER en la base, active=true.
B: ROLE_USER en la sesión, ADMIN en la base, active=true.
C: ROLE_ADMIN en la sesión, ADMIN en la base, active=false.
D: ROLE_ADMIN en la sesión, ADMIN en la base, active=true.
```

¿Quién puede entrar a `/admin/users` sin autenticarse otra vez? Explica la decisión utilizando las **dos verificaciones** de `CurrentAdminAuthorization`.

---

## ✅ Qué debes recordar

Una administración segura no consiste en añadir una tabla y un selector de roles.

**La interfaz facilita la operación; Spring Security protege la ruta; el Service protege las reglas; las transacciones coordinan el cambio; y la auditoría conserva el evento relevante.**

La solución de PetMatch funciona como referencia para entender mecanismos, no como una arquitectura que debas copiar sin evaluar el riesgo, la escala y las necesidades de otra aplicación.

### Para continuar

- [25 — Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md).
- [26 — CSRF, sesión y seguridad web](../04-seguridad/26-csrf-sesion-y-seguridad-web.md).
- [14 — Transacciones y consistencia](../02-dominio-y-persistencia/14-transacciones-y-consistencia.md).
- [16 — Concurrencia y locking](../02-dominio-y-persistencia/16-concurrencia-y-locking.md).
- [32 — Pruebas de integración](../06-calidad-y-recorrido/32-pruebas-de-integracion.md).

### Referencias oficiales

- [Spring Security — AuthorizationManager y autorización HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).
- [Spring Security — Protección CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- [Spring Data JPA — Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).
