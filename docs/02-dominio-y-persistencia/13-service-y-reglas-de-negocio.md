# 13 — Service y reglas de negocio

En el capítulo anterior vimos que los repositories responden preguntas de persistencia:

```text
buscar
comprobar existencia
contar
guardar
eliminar
```

Pero una aplicación real no consiste únicamente en leer y escribir filas.

PetMatch necesita decisiones como:

```text
¿puede este usuario editar esta mascota?
¿puede postularse a esta solicitud?
¿la solicitud sigue abierta?
¿la fecha todavía está vigente?
¿ya existe una postulación previa?
¿puede aceptarse esta postulación?
¿qué debe ocurrir con las demás postulaciones?
```

La pregunta central de este capítulo es:

> **¿Dónde deben vivir esas decisiones y cómo las organiza PetMatch?**

La respuesta principal está en la capa **Service**.

---

# 1. ¿Qué es una regla de negocio?

Una **regla de negocio** expresa una condición, restricción o consecuencia que pertenece al problema que la aplicación intenta resolver.

Ejemplos de PetMatch:

```text
un usuario no puede postularse a su propia solicitud
una solicitud vencida no acepta postulaciones
una postulación no puede duplicarse
solo el owner puede administrar su solicitud
solo una solicitud OPEN puede cancelarse
solo una solicitud IN_PROGRESS puede completarse
```

Estas reglas seguirían teniendo sentido aunque reemplazáramos Thymeleaf por otro frontend.

Por eso no pertenecen naturalmente a una vista.

---

# 2. Regla de negocio vs validación estructural

Un formulario puede validar:

```java
@NotBlank
@Size(max = 150)
@Future
```

Eso responde preguntas como:

```text
¿el título está vacío?
¿supera la longitud máxima?
¿la fecha es futura?
```

Pero una regla como:

```text
el applicant no puede ser el owner
```

requiere consultar el estado de la aplicación y comparar usuarios.

Entonces:

```text
validación estructural
→ DTO / Bean Validation

regla contextual de negocio
→ Service
```

---

# 3. ¿Por qué no poner todas las reglas en el Controller?

PetMatch tiene dos entradas principales:

```text
MVC
REST
```

Ambas reutilizan Services.

Si la regla “no puedes editar una mascota ajena” viviera solo en `PetController`, la API REST tendría que duplicarla.

Eso produciría:

```text
PetController → regla A
PetRestController → copia de regla A
```

Con Service:

```text
PetController ─────┐
                   ├→ PetService → regla A
PetRestController ─┘
```

La regla se aplica sin importar qué interfaz inició el caso de uso.

---

# 4. `PetService`: ownership y consistencia básica

Archivo:

```text
src/main/java/com/petmatch/community/service/PetService.java
```

Dependencias reales:

```java
private final PetRepository petRepository;
private final SupportRequestRepository supportRequestRepository;
private final UserService userService;
```

Esto ya sugiere tres responsabilidades:

```text
mascotas
relación con solicitudes
usuario actual
```

---

# 5. Regla: solo trabajar con mascotas propias

Código real:

```java
@Transactional(readOnly = true)
public Pet findOwnedPet(Long petId, Authentication authentication) {
    User owner = userService.getCurrentUser(authentication);
    return petRepository.findByIdAndOwnerId(petId, owner.getId())
        .orElseThrow(() -> new PetNotFoundException(petId));
}
```

El Service no busca simplemente:

```java
findById(petId)
```

Busca:

```text
pet.id = petId
AND
pet.owner.id = currentUser.id
```

Eso convierte ownership en una condición del caso de uso.

---

# 6. ¿Por qué responder “not found” ante recurso ajeno?

`findOwnedPet(...)` lanza:

```java
new PetNotFoundException(petId)
```

si la mascota no existe **o no pertenece al usuario**.

Esto evita revelar innecesariamente:

```text
“esa mascota sí existe, pero es de otra persona”
```

La decisión concreta de mapping HTTP se verá después, pero la intención del Service es clara:

```text
para este usuario, ese recurso no es accesible como propio
```

---

# 7. Regla: no eliminar una mascota con solicitudes

Código real:

```java
@Transactional
public void delete(Long petId, Authentication authentication) {
    Pet pet = findOwnedPet(petId, authentication);
    if (supportRequestRepository.existsByPetId(pet.getId())) {
        throw new PetDeletionException(pet.getId());
    }
    petRepository.delete(pet);
}
```

La regla es:

```text
si existen SupportRequest asociadas
→ no eliminar Pet
```

¿Por qué vive aquí?

Porque no es una propiedad de la interfaz web.

Es una condición funcional para preservar consistencia del dominio.

---

# 8. `UserService`: registro e identidad actual

Archivo:

```text
src/main/java/com/petmatch/community/service/UserService.java
```

Reglas visibles:

```text
normalizar email
no permitir duplicado
codificar password
resolver usuario autenticado
```

---

# 9. Regla: email normalizado

Código real:

```java
private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
}
```

Antes de consultar o guardar, PetMatch convierte el email a una forma consistente.

Esto evita tratar como usuarios conceptualmente distintos:

```text
USER@example.com
user@example.com
 user@example.com 
```

según la lógica de la aplicación.

---

# 10. Regla: no registrar email duplicado

Código real:

```java
if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
    throw new DuplicateEmailException(normalizedEmail);
}
```

Aquí el Repository responde:

```text
¿existe?
```

El Service decide:

```text
si existe → rechazar registro
```

Esta separación es central:

```text
Repository → dato
Service → significado del dato
```

---

# 11. Regla: nunca guardar la contraseña cruda

Código real:

```java
User user = new User(
    form.getName().trim(),
    normalizedEmail,
    passwordEncoder.encode(form.getPassword())
);
```

La Entity recibe:

```text
passwordHash
```

no la contraseña original.

Aunque profundizaremos seguridad después, esto ya es parte del flujo de negocio del registro.

---

# 12. `SupportRequestService`: solicitudes y estados

Archivo:

```text
src/main/java/com/petmatch/community/service/SupportRequestService.java
```

Dependencias:

```java
SupportRequestRepository
SupportApplicationRepository
PetService
UserService
```

El Service coordina:

- ownership;
- estado de la solicitud;
- mascota asociada;
- postulaciones relacionadas.

---

# 13. Regla: listar solo solicitudes OPEN futuras

Código real:

```java
@Transactional(readOnly = true)
public List<SupportRequest> findOpenRequests() {
    return supportRequestRepository
        .findByStatusAndServiceDateAfterOrderByServiceDateAsc(
            SupportRequestStatus.OPEN,
            LocalDateTime.now()
        );
}
```

La noción funcional de “solicitud abierta visible” incluye:

```text
status = OPEN
+
serviceDate > ahora
```

No basta con mirar el enum.

El tiempo también forma parte de la regla.

---

# 14. Regla: crear una solicitud solo para una mascota propia

Código real:

```java
public SupportRequest create(
    SupportRequestForm form,
    Authentication authentication
) {
    User owner = userService.getCurrentUser(authentication);
    Pet pet = petService.findOwnedPet(form.getPetId(), authentication);

    return supportRequestRepository.save(
        new SupportRequest(..., pet, owner)
    );
}
```

El Service no acepta ciegamente un `petId` enviado por el cliente.

Lo valida mediante:

```text
findOwnedPet
```

Esto evita que un usuario publique una solicitud sobre una mascota ajena modificando el id del formulario o request JSON.

---

# 15. Regla: solo una solicitud OPEN puede modificarse

Código real:

```java
private void requireOpen(SupportRequest request) {
    if (request.getStatus() != SupportRequestStatus.OPEN) {
        throw new SupportRequestStateException(request.getId());
    }
}
```

`update(...)` utiliza:

```java
requireOpen(request);
```

La regla es explícita:

```text
OPEN → editable
otro estado → no editable
```

---

# 16. Regla: cancelar también afecta postulaciones

Código real:

```java
@Transactional
public void cancel(Long requestId, Authentication authentication) {
    SupportRequest request = findOwnedRequest(requestId, authentication);
    requireOpen(request);
    request.setStatus(SupportRequestStatus.CANCELLED);

    supportApplicationRepository
        .findBySupportRequestIdAndStatus(
            request.getId(),
            SupportApplicationStatus.PENDING
        )
        .forEach(application ->
            application.setStatus(SupportApplicationStatus.REJECTED)
        );
}
```

No basta con:

```text
request.status = CANCELLED
```

También hay que resolver el estado de postulaciones que seguían pendientes.

La operación mantiene coherencia entre entidades relacionadas.

---

# 17. ¿Qué ocurriría si no rechazáramos las PENDING?

Podríamos terminar con:

```text
SupportRequest = CANCELLED
SupportApplication = PENDING
```

Eso sería semánticamente contradictorio.

Una postulación no puede seguir esperando una decisión para una solicitud cancelada.

Por eso la regla modifica ambas partes.

---

# 18. Regla: completar solo desde `IN_PROGRESS`

Código real:

```java
@Transactional
public void complete(Long requestId, Authentication authentication) {
    SupportRequest request = findOwnedRequest(requestId, authentication);

    if (request.getStatus() != SupportRequestStatus.IN_PROGRESS) {
        throw new SupportRequestStateException(requestId);
    }

    request.setStatus(SupportRequestStatus.COMPLETED);
}
```

La transición permitida es:

```text
IN_PROGRESS → COMPLETED
```

No:

```text
OPEN → COMPLETED
CANCELLED → COMPLETED
```

Esto ya anticipa el capítulo de máquinas de estado.

---

# 19. Visibilidad de solicitudes no abiertas

Código real resumido de `findVisibleRequest(...)`:

```java
SupportRequest request = findById(requestId);
User currentUser = userService.getCurrentUser(authentication);

boolean owner = request.getOwner().getId().equals(currentUser.getId());
boolean applicant = supportApplicationRepository
    .existsByApplicantIdAndSupportRequestId(
        currentUser.getId(),
        requestId
    );

if (request.getStatus() != SupportRequestStatus.OPEN
    && !owner
    && !applicant) {
    throw new SupportRequestNotFoundException(requestId);
}
```

Regla:

```text
OPEN
→ visible según flujo público autenticado

no OPEN
→ solo owner o applicant relacionado
```

Esto combina:

```text
estado
+
relación del usuario con el recurso
```

---

# 20. `SupportApplicationService`: reglas más complejas

Archivo:

```text
src/main/java/com/petmatch/community/service/SupportApplicationService.java
```

Aquí encontramos el caso de uso más rico del MVP:

```text
apply
accept
reject
```

---

# 21. Regla: solo postularse a solicitudes abiertas y vigentes

Código real:

```java
if (request.getStatus() != SupportRequestStatus.OPEN
    || !request.getServiceDate().isAfter(LocalDateTime.now())) {
    throw new SupportApplicationRuleException(
        "La solicitud ya no acepta postulaciones."
    );
}
```

Deben cumplirse ambas condiciones:

```text
OPEN
AND
serviceDate futura
```

Una solicitud puede conservar técnicamente estado `OPEN`, pero si la fecha ya pasó no debe aceptar nuevas postulaciones.

---

# 22. Regla: no postularse a la solicitud propia

Código real:

```java
if (request.getOwner().getId().equals(applicant.getId())) {
    throw new SupportApplicationRuleException(
        "No puedes postularte a tu propia solicitud."
    );
}
```

Este es un excelente ejemplo de regla que no puede resolverse solo con anotaciones de formulario.

Necesita comparar:

```text
request.owner.id
vs
current applicant.id
```

---

# 23. Regla: no duplicar postulación

Código real:

```java
if (supportApplicationRepository
    .existsByApplicantIdAndSupportRequestId(
        applicant.getId(),
        requestId
    )) {
    throw new SupportApplicationRuleException(
        "Ya te postulaste a esta solicitud."
    );
}
```

Además, la tabla tiene una restricción UNIQUE para:

```text
applicant_id + support_request_id
```

Eso da dos barreras:

```text
Service → mensaje/regla anticipada
DB → garantía final de unicidad
```

---

# 24. Regla: solo el owner puede ver las postulaciones recibidas

Código real:

```java
User owner = userService.getCurrentUser(authentication);

SupportRequest request = supportRequestRepository
    .findByIdAndOwnerId(requestId, owner.getId())
    .orElseThrow(() ->
        new SupportRequestNotFoundException(requestId)
    );
```

Después:

```java
return supportApplicationRepository
    .findBySupportRequestIdOrderByAppliedAtAsc(request.getId());
```

Antes de listar aplicaciones, el Service confirma ownership de la solicitud.

---

# 25. Aceptar una postulación: el caso de uso completo

El método `accept(...)` implementa varias reglas dentro de una única operación.

Secuencia simplificada:

```text
1. identificar owner actual
2. buscar application perteneciente a una request de ese owner
3. bloquear/cargar request
4. validar request OPEN
5. validar application PENDING
6. comprobar que no existe otra ACCEPTED
7. selected → ACCEPTED
8. request → IN_PROGRESS
9. otras PENDING → REJECTED
```

Este flujo es el corazón transaccional del proyecto.

---

# 26. Primera protección: ownership de la postulación

Código real:

```java
SupportApplication application = supportApplicationRepository
    .findByIdAndSupportRequestOwnerId(
        applicationId,
        owner.getId()
    )
    .orElseThrow(() ->
        new SupportApplicationNotFoundException(applicationId)
    );
```

No basta con conocer:

```text
applicationId
```

Debe pertenecer a una solicitud cuyo owner sea el usuario actual.

---

# 27. Segunda protección: estado de request y application

Código real:

```java
if (request.getStatus() != SupportRequestStatus.OPEN
    || application.getStatus() != SupportApplicationStatus.PENDING) {
    throw new SupportApplicationStateException(applicationId);
}
```

Solo puede aceptarse:

```text
request OPEN
+
application PENDING
```

---

# 28. Tercera protección: no tener otra aceptada

Código real:

```java
if (supportApplicationRepository
    .countBySupportRequestIdAndStatus(
        request.getId(),
        SupportApplicationStatus.ACCEPTED
    ) > 0) {
    throw new SupportApplicationStateException(applicationId);
}
```

La regla funcional es:

```text
máximo una application ACCEPTED por request
```

El locking que refuerza este escenario concurrente se verá después.

---

# 29. Las consecuencias de aceptar

Código real:

```java
application.setStatus(SupportApplicationStatus.ACCEPTED);
request.setStatus(SupportRequestStatus.IN_PROGRESS);
```

Y luego:

```java
supportApplicationRepository
    .findBySupportRequestIdAndStatus(
        request.getId(),
        SupportApplicationStatus.PENDING
    )
    .stream()
    .filter(other -> !other.getId().equals(applicationId))
    .forEach(other ->
        other.setStatus(SupportApplicationStatus.REJECTED)
    );
```

Resultado coherente:

```text
selected       → ACCEPTED
request        → IN_PROGRESS
other PENDING  → REJECTED
```

---

# 30. ¿Por qué no dejar otras PENDING?

Porque una vez elegida una persona, la solicitud ya no está buscando ayuda.

Dejar otras aplicaciones pendientes produciría un estado engañoso:

```text
request = IN_PROGRESS
application B = ACCEPTED
application C = PENDING  ← todavía parece esperando decisión
```

El Service resuelve todas las consecuencias del caso de uso.

---

# 31. Rechazar una postulación

Código real:

```java
if (application.getSupportRequest().getStatus()
        != SupportRequestStatus.OPEN
    || application.getStatus()
        != SupportApplicationStatus.PENDING) {
    throw new SupportApplicationStateException(applicationId);
}

application.setStatus(SupportApplicationStatus.REJECTED);
```

Solo una postulación pendiente de una solicitud aún abierta puede rechazarse manualmente mediante este caso de uso.

---

# 32. El Service como orquestador

Observa `accept(...)`.

El Service no se limita a una sola Entity.

Coordina:

```text
User
SupportApplication
SupportRequest
otras SupportApplication
repositories
estado
ownership
concurrencia
```

Eso es **orquestación de un caso de uso**.

Un Service puede necesitar varias colaboraciones porque el caso de uso atraviesa varias piezas del dominio.

---

# 33. Service no significa “clase con todo”

Que el Service coordine reglas no implica crear un `MegaService`.

PetMatch separa:

```text
UserService
PetService
SupportRequestService
SupportApplicationService
```

Cada uno se organiza alrededor de un área funcional coherente.

---

# 34. Métodos privados como reglas reutilizables internas

`SupportRequestService` contiene:

```java
private void requireOpen(SupportRequest request) {
    if (request.getStatus() != SupportRequestStatus.OPEN) {
        throw new SupportRequestStateException(request.getId());
    }
}
```

Esto evita repetir la misma condición en varios métodos de la clase.

El nombre:

```text
requireOpen
```

expresa intención de negocio mejor que repetir un `if` sin contexto.

---

# 35. Normalización también pertenece al caso de uso

Services contienen helpers como:

```java
private String normalize(String value) {
    return value.trim();
}
```

o:

```java
private String normalizeNullable(String value) {
    ...
}
```

La aplicación decide cómo almacenar entradas textuales para evitar espacios accidentales.

No toda lógica de Service tiene que ser una “gran regla”.

También puede preparar datos de forma consistente.

---

# 36. Excepciones expresan fallos del dominio

PetMatch tiene excepciones como:

```text
PetDeletionException
PetNotFoundException
SupportRequestNotFoundException
SupportRequestStateException
SupportApplicationRuleException
SupportApplicationStateException
DuplicateEmailException
```

Esto permite que el Service diga:

```text
qué salió mal
```

sin decidir necesariamente cómo se representa en HTML o JSON.

Después otras capas pueden mapear ese fallo a:

```text
mensaje web
HTTP 404
HTTP 409
ProblemDetail
```

---

# 37. No usar `boolean` para todos los fallos

Podríamos diseñar:

```java
boolean accept(...)
```

y devolver `false` para cualquier problema.

Pero perderíamos la diferencia entre:

```text
no encontrado
estado inválido
regla violada
```

Las excepciones específicas conservan mejor el significado.

---

# 38. La regla debe sobrevivir a otra interfaz

Haz esta prueba mental:

> Si mañana quitamos Thymeleaf y usamos otra interfaz, ¿esta regla seguiría siendo necesaria?

Si la respuesta es sí, probablemente no debería estar únicamente en la View/Controller.

Ejemplos:

```text
no duplicar postulación → sí
solo owner puede editar → sí
status OPEN requerido → sí
color del botón → no
texto del encabezado → no
```

---

# 39. Service y seguridad

Hay dos niveles que no debemos confundir:

## Seguridad de rutas

Spring Security puede decir:

```text
ruta requiere autenticación
```

## Autorización sobre recurso

El Service debe decir:

```text
esta Pet pertenece a este User
esta SupportRequest pertenece a este owner
```

Estar autenticado no significa poder modificar cualquier registro.

PetMatch combina ambos niveles.

---

# 40. Regla en UI no es suficiente

Supón que el template oculta el botón:

```text
Editar
```

para mascotas ajenas.

Un usuario todavía podría intentar enviar manualmente:

```text
POST /pets/123
```

Por eso la regla real debe estar en backend.

`findOwnedPet(...)` protege el caso de uso aunque el cliente sea manipulado.

---

# 41. ¿Por qué algunos métodos devuelven Entity?

Los Services actuales devuelven Entities en varios casos:

```java
public Pet create(...)
public Pet update(...)
public SupportRequest findById(...)
```

El Controller o mapper puede usar ese resultado para construir la respuesta.

Esto es una decisión del diseño actual.

No significa que todo Service universalmente deba devolver Entity; en arquitecturas distintas podrían existir modelos/DTO de aplicación específicos.

---

# 42. ¿Por qué `toForm(...)` está en Service?

`PetService` y `SupportRequestService` tienen métodos para convertir una Entity existente en un Form DTO editable.

Ejemplo:

```java
public PetForm toForm(Pet pet) {
    PetForm form = new PetForm();
    ...
    return form;
}
```

En el proyecto actual esta transformación vive en Service.

No debemos afirmar que sea la única ubicación válida en todos los proyectos.

Con mayor complejidad podría extraerse a un mapper dedicado.

---

# 43. Pruebas como documentación de reglas

`SupportApplicationServiceTests` verifica:

```text
accept
→ selected ACCEPTED
→ request IN_PROGRESS
→ otherPending REJECTED
```

También verifica que una solicitud vencida no acepte nuevas postulaciones.

`SupportRequestServiceTests` verifica:

```text
cancel
→ request CANCELLED
→ pendingApplication REJECTED
```

Las pruebas permiten expresar una regla en formato ejecutable.

---

# 44. Regla + test

Una práctica importante es pensar cada regla con al menos:

```text
caso válido
caso inválido
consecuencia esperada
```

Ejemplo:

```text
Regla:
solo request OPEN acepta aplicaciones

válido:
OPEN + fecha futura

inválido:
OPEN + fecha pasada

resultado inválido:
SupportApplicationRuleException
```

Eso facilita convertir reglas en pruebas unitarias.

---

# 45. Mapa de reglas principales

| Service | Regla destacada |
|---|---|
| `UserService` | email único/normalizado, password encoded |
| `PetService` | ownership, no borrar si tiene requests |
| `SupportRequestService` | owner, OPEN para editar/cancelar, IN_PROGRESS para completar |
| `SupportApplicationService` | request vigente, no self-apply, no duplicado, una aceptación, rechazo de otras |

---

# 46. ⚠️ Errores frecuentes

## Error 1 — Poner reglas solo en el Controller

Provoca duplicación entre MVC y REST.

## Error 2 — Confiar en ids enviados por el cliente

Un `petId` debe comprobar ownership antes de usarse.

## Error 3 — Confundir autenticación con ownership

Usuario autenticado no significa propietario del recurso.

## Error 4 — Poner reglas complejas dentro del Repository

El Repository debe responder operaciones de persistencia; el Service interpreta y coordina.

## Error 5 — Validar solo en frontend

El cliente puede manipularse.

## Error 6 — Cambiar un estado sin resolver consecuencias relacionadas

Cancelar una request requiere resolver applications PENDING.

## Error 7 — Devolver `false` para todos los fallos

Pierde significado frente a excepciones específicas.

## Error 8 — Crear un único Service gigante

Service no es sinónimo de “todo lo que no sé dónde poner”.

## Error 9 — Creer que una restricción de DB reemplaza la regla de aplicación

La DB garantiza integridad, pero el Service permite anticipar el caso y producir comportamiento de dominio más claro.

## Error 10 — Copiar una regla entre MVC y REST

El Service compartido existe precisamente para evitarlo.

---

# 47. 🛠 Prueba en el código

## Actividad 1 — Inventario de reglas

Abre los cuatro Services y crea una tabla:

```text
método | precondición | acción | excepción posible
```

## Actividad 2 — Sigue ownership

Traza:

```text
PetController.update
→ PetService.update
→ findOwnedPet
→ PetRepository.findByIdAndOwnerId
```

Explica dónde queda protegida realmente la mascota.

## Actividad 3 — Descompón `apply`

Escribe en pseudocódigo cada `if` de:

```text
SupportApplicationService.apply
```

Sin copiar Java.

## Actividad 4 — Descompón `accept`

Dibuja estados antes y después con tres applicants:

```text
A PENDING
B PENDING
C PENDING
```

si B es aceptado.

## Actividad 5 — Pruebas

Abre:

```text
SupportApplicationServiceTests.java
SupportRequestServiceTests.java
```

y relaciona cada `verify(...)` con una regla funcional.

---

# 48. 🧪 Comprueba que entendiste

1. ¿Qué diferencia hay entre validación estructural y regla de negocio?
2. ¿Por qué las reglas no deberían vivir únicamente en MVC Controllers?
3. ¿Cómo protege PetMatch ownership de una mascota?
4. ¿Qué impide eliminar una Pet con solicitudes?
5. ¿Qué hace `UserService` antes de guardar un email?
6. ¿Por qué se comprueba duplicidad de email en Service si también hay UNIQUE en DB?
7. ¿Qué condiciones hacen que una solicitud aparezca como abierta/listable?
8. ¿Qué estado necesita una request para editarse/cancelarse?
9. ¿Qué ocurre con las applications PENDING al cancelar?
10. ¿Desde qué estado puede completarse una request?
11. ¿Qué tres reglas principales comprueba `apply(...)` antes de guardar?
12. ¿Quién puede ver las postulaciones recibidas?
13. ¿Qué condiciones debe cumplir una application para ser aceptada?
14. ¿Qué ocurre con las otras PENDING después de aceptar una?
15. ¿Por qué ocultar un botón no es suficiente autorización?

### Respuestas esperadas

1. La validación comprueba estructura de entrada; la regla usa contexto/estado del dominio.
2. Porque MVC y REST deben reutilizar la misma lógica.
3. Mediante `findByIdAndOwnerId` usando el usuario autenticado.
4. `existsByPetId` + `PetDeletionException`.
5. `trim()` y lowercase con `Locale.ROOT`.
6. Para detectar la regla anticipadamente y mantener además una garantía final de integridad en DB.
7. `OPEN` y `serviceDate` futura.
8. `OPEN`.
9. Pasan a `REJECTED`.
10. `IN_PROGRESS`.
11. Request OPEN/vigente, no ser owner, no haber aplicado antes.
12. El owner de la request.
13. Request OPEN, application PENDING y no existir otra ACCEPTED.
14. Se vuelven `REJECTED`.
15. Porque el cliente puede manipular peticiones; la regla debe ejecutarse en backend.

---

# 49. ✅ Qué debes recordar

- **El Service expresa y coordina casos de uso y reglas del dominio.**
- La validación de DTO no reemplaza reglas contextuales.
- MVC y REST reutilizan Services para evitar duplicar reglas.
- Ownership debe comprobarse en backend.
- Repositories responden preguntas de datos; Services interpretan su significado.
- PetMatch evita eliminar Pets con SupportRequests.
- Emails se normalizan y no pueden duplicarse.
- Una solicitud listable debe estar OPEN y tener fecha futura.
- Solo solicitudes OPEN se editan/cancelan.
- Cancelar rechaza postulaciones pendientes.
- Solo `IN_PROGRESS` puede pasar a `COMPLETED`.
- No se puede aplicar a una request propia, vencida o duplicada.
- Aceptar una application cambia varios objetos de forma coordinada.
- Solo una application queda ACCEPTED; otras pendientes se rechazan.
- Las excepciones específicas preservan el significado del fallo.
- Las pruebas unitarias son documentación ejecutable de reglas importantes.

---

# 🔗 Continúa con

Ya entendemos **qué reglas debe cumplir una operación**.

Ahora aparece una pregunta crítica:

> **¿Qué sucede si una operación modifica varias entidades y falla a mitad de camino? ¿Cómo evitamos dejar la base de datos en un estado parcial?**

Eso nos lleva a:

**[Capítulo 14 — Transacciones y consistencia →](14-transacciones-y-consistencia.md)**

---

[← Capítulo 12 — Spring Data JPA](12-spring-data-jpa.md) · [Índice del bloque](README.md) · [Índice general](../README.md) · [Siguiente → Capítulo 14](14-transacciones-y-consistencia.md)
