# 35 — Errores frecuentes al trabajar con Spring Boot en PetMatch

Este capítulo cierra el bloque de calidad y recorrido completo.

Hasta ahora estudiamos:

```text
cómo está construido PetMatch
→ cómo se prueba
→ cómo fluye de punta a punta
→ qué decisiones técnicas utiliza
```

Ahora hacemos algo diferente:

> **convertir los errores más probables de un aprendiz en señales de diagnóstico.**

No todos los errores producen una excepción inmediata. Algunos son más peligrosos porque el programa puede compilar y aun así quedar mal diseñado, inseguro o difícil de mantener.

La idea de este capítulo es aprender a reconocerlos antes de que se conviertan en problemas grandes.

---

# 1. Error: aprender anotaciones sin entender responsabilidades

Un aprendiz puede memorizar:

```text
@Controller
@Service
@Repository
@Entity
@Transactional
```

pero no saber por qué existen.

Eso termina en clases con responsabilidades mezcladas.

La pregunta correcta no es:

> ¿qué anotación pongo aquí?

Sino:

> ¿qué responsabilidad tiene esta pieza?

En PetMatch:

```text
Controller
→ interfaz HTTP

Service
→ caso de uso y reglas

Repository
→ acceso a datos

Entity
→ estado persistente del dominio

DTO
→ transferencia de datos entre fronteras
```

Relacionado: [07 — Arquitectura por capas](../01-fundamentos/07-arquitectura-por-capas.md).

---

# 2. Error: poner reglas de negocio en el Controller

Ejemplo problemático:

```java
if (request.getStatus() != OPEN) {
    ...
}
if (currentUserIsOwner) {
    ...
}
```

repetido en Controllers MVC y REST.

¿Por qué es un problema?

Porque PetMatch tiene dos interfaces:

```text
MVC
REST
```

Si cada Controller implementara reglas, habría dos versiones del negocio.

El diseño real evita esto:

```text
PetController ───────┐
                     ├→ PetService
PetRestController ───┘
```

La misma idea aplica a requests y applications.

Relacionado: [13 — Service y reglas de negocio](../02-dominio-y-persistencia/13-service-y-reglas-de-negocio.md).

---

# 3. Error: Controller → Repository para todo

No es ilegal que un Controller use un Repository en cualquier aplicación imaginable, pero en PetMatch sería una señal de que estamos saltando la capa donde viven reglas importantes.

Ejemplo peligroso:

```text
Controller
→ supportRequestRepository.findById(...)
→ cambiar status
→ save
```

Eso podría omitir:

```text
ownership
estado permitido
rechazo de otras applications
locking
visibilidad
```

El flujo crítico debe pasar por el Service correspondiente.

---

# 4. Error: crear dependencias con `new` dentro de un Service

Ejemplo incorrecto:

```java
public class PetService {
    private final PetRepository repository =
        new PetRepository(...);
}
```

Los Repositories de Spring Data son administrados por Spring.

PetMatch usa constructor injection:

```text
Spring crea dependencias
→ las entrega al constructor
```

Esto también facilita unit testing.

Relacionado: [08 — Inyección de dependencias](../01-fundamentos/08-inyeccion-de-dependencias.md).

---

# 5. Error: pensar que Spring “inyecta cualquier cosa” mágicamente

Dependency Injection no significa que cualquier objeto aparezca por arte de magia.

Spring debe conocer cómo construir la dependencia.

Ejemplos de objetos administrados:

```text
@Service
@Controller
@Configuration
@Repository generado por Spring Data
@Bean
```

Un `PetForm` creado por binding de una request no es el mismo tipo de Bean singleton administrado como Service.

---

# 6. Error: usar Entity como Form DTO

Podríamos intentar:

```java
@PostMapping
public String create(@ModelAttribute Pet pet) {
    ...
}
```

Pero `Pet` contiene responsabilidades de persistencia y relaciones que el formulario no debería controlar directamente.

PetMatch separa:

```text
Pet
PetForm
```

El formulario controla solo los datos editables permitidos.

Relacionado: [20 — Formularios y Form DTO](../03-web-mvc/20-formularios-y-form-dto.md).

---

# 7. Error: devolver Entities directamente por REST

Ejemplo tentador:

```java
@GetMapping
public List<Pet> findAll() {
    return repository.findAll();
}
```

Puede provocar:

```text
exposición accidental de relaciones
grafos JSON inesperados
acoplamiento API ↔ persistencia
lazy loading fuera de la transacción
datos que el cliente no debería conocer
```

PetMatch usa response DTO explícitos.

Relacionado: [28 — DTO REST, JSON y mapping](../05-rest/28-dto-rest-json-y-mapping.md).

---

# 8. Error: permitir que el cliente envíe `ownerId`

Una request como:

```json
{
  "name": "Luna",
  "ownerId": 99
}
```

sería peligrosa si el backend confiara en ese `ownerId`.

En PetMatch el owner sale de:

```text
Authentication
→ UserService.getCurrentUser
```

No de un campo controlado por el cliente.

Relacionado: [25 — Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md).

---

# 9. Error: creer que autenticación = autorización

Un usuario puede estar autenticado y aun así no tener derecho a modificar una Pet concreta.

```text
Authentication válida
≠
puede usar cualquier id
```

Por eso PetMatch consulta:

```text
findByIdAndOwnerId
```

y equivalentes.

Relacionado: [25 — Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md).

---

# 10. Error: proteger ownership solo ocultando botones

Ocultar:

```html
<button>Eliminar</button>
```

no impide que alguien construya manualmente la request HTTP.

La autorización debe existir en backend.

PetMatch valida ownership en Service/Repository.

La vista es experiencia de usuario; el backend es la frontera de seguridad.

---

# 11. Error: hacer `findById(id)` y comprobar ownership demasiado tarde

Una alternativa común es:

```text
findById
→ cargar recurso
→ if owner mismatch
```

Puede funcionar si se implementa correctamente, pero PetMatch usa con frecuencia una consulta más defensiva:

```text
findByIdAndOwnerId
```

Así la misma consulta expresa:

```text
recurso + relación permitida
```

---

# 12. Error: pensar que `@Valid` reemplaza las reglas del Service

`@Valid` puede comprobar:

```text
@NotBlank
@Email
@Size
@Future
```

Pero no responde automáticamente:

```text
¿esta Pet es mía?
¿ya me postulé?
¿soy el owner?
¿la request sigue OPEN?
¿ya existe una ACCEPTED?
```

Bean Validation y reglas de negocio son capas distintas.

Relacionado: [21 — Validación](../03-web-mvc/21-validacion.md).

---

# 13. Error: mover toda la validación de negocio a anotaciones

No toda regla puede o debe expresarse como:

```java
@Something
```

Ejemplo:

```text
owner no puede postularse a su propia request
```

requiere consultar identidad y relación entre Entities.

`SupportApplicationService.apply(...)` es el lugar adecuado para coordinar esa decisión.

---

# 14. Error: confiar solo en la base de datos para reglas entendibles

PetMatch tiene una restricción única applicant/request.

Pero además el Service pregunta:

```text
existsByApplicantIdAndSupportRequestId
```

¿Por qué ambos niveles?

```text
Service
→ regla explícita y controlada

DB
→ última garantía de integridad
```

Esperar únicamente un error SQL suele producir peor experiencia y menor claridad de negocio.

---

# 15. Error: creer que una comprobación previa elimina toda condición de carrera

Patrón insuficiente por sí solo:

```text
if countAccepted == 0
→ aceptar
```

Dos transacciones podrían leer `0` casi al mismo tiempo.

Por eso el flujo crítico de `accept` incluye:

```text
PESSIMISTIC_WRITE sobre SupportRequest
→ volver a comprobar estado
→ count accepted
→ cambiar estados
```

Relacionado: [16 — Concurrencia y locking](../02-dominio-y-persistencia/16-concurrencia-y-locking.md).

---

# 16. Error: decir que el lock garantiza cualquier problema de concurrencia

El lock actual protege un caso muy concreto:

```text
aceptación concurrente alrededor de una SupportRequest
```

No significa:

```text
“toda la aplicación es thread-safe”
```

ni reemplaza diseño transaccional, constraints o pruebas.

---

# 17. Error: afirmar que la concurrencia real está probada

Los unit tests mockean Repository.

`MvpFlowIntegrationTests` ejecuta el flujo de manera secuencial.

Por tanto:

```text
lock implementado ✅
prueba con dos transacciones simultáneas ❌
```

Relacionado: [32 — Pruebas de integración](32-pruebas-de-integracion.md).

---

# 18. Error: cambiar estados directamente desde cualquier parte

Ejemplo peligroso:

```java
request.setStatus(COMPLETED);
```

sin comprobar desde qué estado viene.

PetMatch trata estados como una máquina:

```text
OPEN → IN_PROGRESS → COMPLETED
OPEN → CANCELLED
```

Los Services gobiernan las transiciones del caso actual.

Relacionado: [15 — Máquinas de estado](../02-dominio-y-persistencia/15-maquinas-de-estado.md).

---

# 19. Error: confundir estado de request y estado de application

Son dos máquinas relacionadas.

`SupportRequestStatus`:

```text
OPEN
IN_PROGRESS
COMPLETED
CANCELLED
```

`SupportApplicationStatus`:

```text
PENDING
ACCEPTED
REJECTED
```

Aceptar una application produce cambios coordinados en ambas.

---

# 20. Error: aceptar una application y olvidar las otras PENDING

Si B pasa a:

```text
ACCEPTED
```

pero C continúa:

```text
PENDING
```

la UI y el dominio quedarían ambiguos.

El flujo real rechaza las otras pendientes cuando se acepta una.

Relacionado: [33 — Flujo completo PetMatch](33-flujo-completo-petmatch.md).

---

# 21. Error: permitir self-apply porque “el botón no aparece”

El owner podría construir la request manualmente.

La regla debe estar en backend:

```text
request.owner.id == applicant.id
→ SupportApplicationRuleException
```

PetMatch la implementa en `SupportApplicationService`.

---

# 22. Error: olvidar la fecha del servicio

No basta con que una request tenga status:

```text
OPEN
```

Para **postularse (`apply`)** a una solicitud, el Service también comprueba que:

```text
serviceDate > now
```

Una solicitud temporalmente vencida no debe seguir aceptando nuevas postulaciones.

La operación `accept(...)` valida otras condiciones: request `OPEN`, application `PENDING` y ausencia de otra application ya `ACCEPTED`.

---

# 23. Error: creer que `LocalDateTime` incluye timezone

`LocalDateTime` representa fecha y hora sin zona/offset.

Por tanto no debemos afirmar que los valores de PetMatch son automáticamente:

```text
UTC
America/Bogota
Z
+00:00
```

si el contrato actual no expresa eso.

Relacionado: [28 — DTO REST](../05-rest/28-dto-rest-json-y-mapping.md).

---

# 24. Error: usar password en texto plano

Nunca debería aparecer lógica como:

```java
user.setPasswordHash(form.getPassword());
```

PetMatch usa:

```text
PasswordEncoder.encode(...)
```

Relacionado: [24 — Contraseñas y PasswordEncoder](../04-seguridad/24-contrasenas-y-password-encoder.md).

---

# 25. Error: comparar raw password con hash usando `equals`

Incorrecto:

```java
raw.equals(passwordHash)
```

La verificación pertenece al mecanismo de `PasswordEncoder`/Spring Security.

La aplicación no implementa su propio algoritmo de comparación manual.

---

# 26. Error: transformar el password con `trim()` por costumbre

PetMatch normaliza nombres, descripciones y email donde corresponde.

Pero `UserService.register(...)` no hace `trim()` del password.

Eso es importante: modificar silenciosamente un secreto puede cambiar la credencial que el usuario creyó establecer.

No reutilices una función genérica de normalización de strings para passwords sin una decisión explícita.

---

# 27. Error: crear un `POST /login` manual sin entender Spring Security

`AuthController` expone la página de login, pero el procesamiento del login es manejado por Spring Security según:

```java
.formLogin(...)
.loginPage("/login")
.usernameParameter("email")
```

No existe un método productivo del Controller que reciba email/password y compare hashes manualmente.

Relacionado: [22 — Autenticación](../04-seguridad/22-autenticacion.md).

---

# 28. Error: pensar que `ADMIN` implica un módulo admin completo

El enum tiene:

```text
USER
ADMIN
```

y `SecurityConfig` contiene:

```text
/admin/** → hasRole("ADMIN")
```

Eso no significa que exista actualmente un panel o módulo administrativo completo.

No inventes Controllers, templates o casos de uso que no están en el repositorio.

---

# 29. Error: desactivar CSRF globalmente porque existe una API REST

PetMatch hace algo mucho más específico.

Chain API:

```text
/api/**
→ CSRF disabled
```

Chain web:

```text
no desactiva CSRF
```

Desactivar CSRF para toda la aplicación cambiaría la política de seguridad de la interfaz web con sesión.

Relacionado: [26 — CSRF, sesión y seguridad web](../04-seguridad/26-csrf-sesion-y-seguridad-web.md).

---

# 30. Error: agregar manualmente `_csrf` a cada template sin revisar integración

Con Spring Security + Spring MVC + Thymeleaf existe integración para forms de métodos inseguros.

La ausencia de una cadena literal `_csrf` escrita a mano en cada template no demuestra automáticamente que la protección no exista.

Primero hay que comprender la infraestructura activa.

---

# 31. Error: pensar que las dos SecurityFilterChain se ejecutan completas para la misma request

La chain API usa:

```java
.securityMatcher("/api/**")
```

y `@Order(1)`.

Una request se procesa por la chain que hace match según la selección de Spring Security.

No debemos imaginar:

```text
API chain
→ después web chain completa
```

sobre la misma request.

Relacionado: [29 — Seguridad REST](../05-rest/29-seguridad-rest.md).

---

# 32. Error: confundir `securityMatcher` con ownership

Esta configuración:

```java
.securityMatcher("/api/**")
.anyRequest().authenticated()
```

responde:

```text
¿esta request API necesita autenticación?
```

No responde:

```text
¿esta Pet pertenece a este usuario?
```

Ownership sigue siendo responsabilidad del caso de uso.

---

# 33. Error: pensar que HTTP Basic cifra las credenciales

HTTP Basic define cómo se presentan las credenciales HTTP.

No cifra por sí mismo el transporte.

Por eso en un entorno real debe usarse sobre HTTPS.

PetMatch usa HTTP local con propósito académico; eso no convierte HTTP Basic en cifrado.

---

# 34. Error: interpretar `STATELESS` como “la aplicación no tiene estado”

`SessionCreationPolicy.STATELESS` se refiere a cómo la chain API maneja la autenticación de seguridad entre requests.

PetMatch sigue teniendo estado persistente:

```text
Users
Pets
SupportRequests
SupportApplications
```

en MySQL.

También siguen existiendo transacciones JPA.

---

# 35. Error: confundir 401, 403, 404 y 409

Una guía mental útil:

```text
401
→ falta/falla autenticación

403
→ identidad conocida pero acceso denegado por política

404
→ recurso no encontrado o no visible según política actual

409
→ operación entra en conflicto con regla/estado
```

PetMatch usa a menudo 404 para ownership/visibilidad de recurso, no necesariamente 403.

Relacionado: [30 — ProblemDetail y errores HTTP](../05-rest/30-problemdetail-y-errores-http.md).

---

# 36. Error: esperar que todos los errores REST tengan el mismo handler

`ApiExceptionHandler` maneja excepciones del flujo REST MVC.

Pero un:

```text
401 por HTTP Basic
```

puede ocurrir antes de llegar al Controller/advice.

No debemos afirmar que todo error de Security pasa por `ApiExceptionHandler`.

---

# 37. Error: capturar `Exception.class` y devolver siempre 400

Eso ocultaría diferencias importantes entre:

```text
bad JSON
validation
not found
business conflict
authentication
```

PetMatch mantiene varias categorías HTTP.

Un handler genérico total tampoco aparece en la implementación actual.

---

# 38. Error: exponer el mensaje técnico de cualquier excepción

El handler actual usa el mensaje de algunas excepciones NotFound, pero para conflictos devuelve un detail genérico.

Esto demuestra una idea importante:

```text
mensaje interno
≠
contrato público obligatorio
```

No toda excepción debería filtrar detalles técnicos al cliente.

---

# 39. Error: creer que `ProblemDetail` valida los datos

`ProblemDetail` representa el error después de que ocurrió.

No realiza Bean Validation ni reglas de negocio.

Flujo:

```text
error ocurre
→ handler lo clasifica
→ ProblemDetail lo representa
```

---

# 40. Error: serializar JSON manualmente

Evita:

```java
return "{\"name\":\"" + pet.getName() + "\"}";
```

Spring convierte response DTO a JSON mediante su infraestructura HTTP.

Concatenar JSON manualmente introduce problemas de escaping, tipos y mantenimiento.

---

# 41. Error: creer que el mapper decide reglas

`ApiDtoMapper` hace:

```text
transformación de forma de datos
```

No debería hacer:

```text
ownership
consultas DB
transiciones de estado
autenticación
```

Mapper y Service tienen responsabilidades diferentes.

---

# 42. Error: asumir MapStruct porque existe un mapper

El mapper actual es manual y estático.

No se observa:

```text
MapStruct
ModelMapper
```

como implementación actual.

Pueden ser evoluciones, no descripciones del presente.

---

# 43. Error: creer que `record` significa Entity inmutable

Los records REST de PetMatch son DTO.

Las Entities JPA son clases diferentes con ciclo de vida persistente, relaciones y cambios de estado.

No mezcles ambos roles.

---

# 44. Error: convertir todas las relaciones a EAGER para evitar LazyInitializationException

Esta “solución” puede terminar cargando datos innecesarios para todas las consultas.

PetMatch mantiene relaciones lazy y usa consultas/`EntityGraph` donde necesita asociaciones concretas.

Relacionado: [17 — Lazy loading y EntityGraph](../02-dominio-y-persistencia/17-lazy-loading-y-entitygraph.md).

---

# 45. Error: activar Open Session in View para esconder un diseño de fetch incorrecto

PetMatch configura:

```yaml
spring.jpa.open-in-view: false
```

Eso obliga a pensar dónde deben cargarse las relaciones.

Si un mapper necesita `request.pet.name`, el Repository/Service debe preparar correctamente ese dato dentro de la frontera de persistencia apropiada.

---

# 46. Error: creer que `EntityGraph` convierte todas las relaciones en EAGER permanentemente

`@EntityGraph` define un plan de carga para consultas concretas.

No cambia mágicamente toda la estrategia global del modelo para cualquier consulta.

---

# 47. Error: pensar que `save()` es obligatorio después de cada setter

Dentro de una transacción, una Entity managed puede persistir cambios mediante dirty checking.

Ejemplo:

```java
request.setStatus(COMPLETED);
```

no necesita necesariamente:

```java
repository.save(request);
```

si la Entity está administrada dentro de la transacción correspondiente.

Relacionado: [14 — Transacciones y consistencia](../02-dominio-y-persistencia/14-transacciones-y-consistencia.md).

---

# 48. Error: pensar que nunca hace falta `save()`

El error contrario también existe.

Para una Entity nueva, PetMatch sí usa:

```text
repository.save(newEntity)
```

Dirty checking no significa que una instancia Java arbitraria no persistida aparezca sola en la base.

---

# 49. Error: mover `@Transactional` al azar

Una transacción debe corresponder a una unidad de trabajo coherente.

En PetMatch las operaciones críticas se agrupan en Services:

```text
accept
cancel
complete
create/update
```

Poner transacciones sin comprender el caso de uso puede ampliar o romper fronteras de consistencia.

---

# 50. Error: confundir `@Transactional` del Service con el del test

En producción:

```text
@Transactional en Service
→ frontera del caso de uso
```

En integración:

```text
@Transactional en test
→ infraestructura/aislamiento de prueba
```

La anotación es la misma, pero el propósito no es idéntico.

---

# 51. Error: asumir cascade/orphanRemoval que no está declarado

Si una relación JPA no declara cierta propagación, no debemos imaginar que:

```text
borrar parent
→ borra todos los children automáticamente
```

El comportamiento debe leerse desde el mapping real.

Esto es especialmente importante antes de diseñar deletes.

---

# 52. Error: borrar Pet sin considerar SupportRequests

`PetService.delete(...)` comprueba:

```text
existsByPetId
```

Si hay requests asociadas lanza:

```text
PetDeletionException
```

El negocio evita eliminar una Pet que ya participa en historial/solicitudes.

---

# 53. Error: creer que `ddl-auto: update` es un sistema de migraciones

Configuración actual:

```yaml
hibernate:
  ddl-auto: update
```

Es conveniente para el proyecto demostrativo.

No equivale a:

```text
Flyway
Liquibase
migrations versionadas
```

Relacionado: [06 — Configuración](../01-fundamentos/06-configuracion-y-application-yaml.md), [39 — Evoluciones](../07-referencia/39-posibles-evoluciones-no-implementadas.md).

---

# 54. Error: creer que `.env` se carga automáticamente por Spring Boot

El repositorio ignora:

```text
.env
```

pero `application.yaml` usa:

```text
${DB_URL}
${DB_USERNAME}
${DB_PASSWORD}
```

Spring Boot no debe documentarse aquí como si leyera automáticamente un archivo `.env` del proyecto.

Las variables deben estar disponibles en el entorno/proceso mediante el mecanismo que utilice el desarrollador.

---

# 55. Error: commitear `.env` con secretos

`.gitignore` incluye:

```text
.env
```

Eso protege el flujo normal contra un commit accidental del archivo local.

Pero recuerda:

```text
.gitignore
≠
secret manager
```

Si un secreto ya fue committeado, agregarlo después al `.gitignore` no elimina su historial.

Este tema continúa en el capítulo 36.

---

# 56. Error: asumir que las pruebas usan H2

No se observa dependencia/configuración H2 específica de test.

Las pruebas `@SpringBootTest` usan el DataSource que pueda construirse con la configuración disponible.

No documentes:

```text
“los tests siempre corren en memoria”
```

porque no describe el repositorio actual.

---

# 57. Error: afirmar que Testcontainers está configurado

No se observa infraestructura Testcontainers actual.

Es una evolución posible, no una característica implementada.

Relacionado: [39 — Evoluciones no implementadas](../07-referencia/39-posibles-evoluciones-no-implementadas.md).

---

# 58. Error: creer que `contextLoads()` prueba todo el sistema

`contextLoads()` responde principalmente:

```text
¿puede levantarse el ApplicationContext?
```

No demuestra:

```text
ownership
accept
REST 201
ProblemDetail 409
concurrencia real
```

Relacionado: [32 — Pruebas de integración](32-pruebas-de-integracion.md).

---

# 59. Error: confundir Mockito con MockMvc

```text
Mockito
→ simula colaboradores Java

MockMvc
→ ejecuta requests en el stack Spring MVC de testing
```

`MockMvc` no significa que todos los Services sean mocks.

---

# 60. Error: decir que MockMvc usa un servidor HTTP externo real

Los tests actuales procesan requests dentro de la infraestructura de testing de Spring MVC.

No dependen de que exista un proceso externo escuchando necesariamente en:

```text
localhost:8080
```

para esas llamadas MockMvc.

---

# 61. Error: medir calidad solo por número de tests

Una suite con muchos tests puede probar poco si las assertions no representan comportamiento importante.

Preguntas mejores:

```text
¿qué regla demuestra?
¿qué capa integra?
¿qué error detectaría?
¿qué caso todavía no cubre?
```

---

# 62. Error: inventar porcentaje de cobertura

El repositorio no debe describirse con:

```text
“90 % de cobertura”
```

si no existe una medición verificada que lo respalde.

El libro documenta casos cubiertos por las pruebas, no porcentajes de cobertura no medidos.

---

# 63. Error: afirmar que todos los endpoints REST tienen integración dedicada

`RestApiIntegrationTests` verifica actualmente algunos comportamientos importantes:

```text
401 sin Basic
200 con Basic
201 create Pet
validation 400
```

No existe un test REST dedicado para cada `PUT`, `DELETE`, `404`, `409`, accept, reject, cancel y complete.

Implementación y cobertura son conceptos distintos.

---

# 64. Error: llamar “end-to-end de navegador” a `MvpFlowIntegrationTests`

Ese test llama Services directamente y construye `Authentication` manualmente.

No usa:

```text
navegador
POST /login
Thymeleaf
form HTTP completo
```

Es una integración del dominio/backend, no E2E de navegador.

---

# 65. Error: confundir código implementado con idea mencionada

Que el libro explique:

```text
JWT
Flyway
Testcontainers
MapStruct
```

como alternativa no significa que el proyecto los use.

Siempre busca una etiqueta mental:

```text
IMPLEMENTADO
vs
POSIBLE EVOLUCIÓN
```

Relacionado: [34 — Buenas prácticas y decisiones](34-buenas-practicas-y-decisiones.md).

---

# 66. Error: documentar F11 File & Image Upload como implementado

El estado actual no contiene:

```text
MultipartFile
upload endpoint
almacenamiento de imágenes
S3
MinIO
Cloudinary
```

La carga de archivos/imágenes debe permanecer únicamente como posible evolución no implementada.

Relacionado: [39 — Evoluciones no implementadas](../07-referencia/39-posibles-evoluciones-no-implementadas.md).

---

# 67. Error: convertir PetMatch en otra aplicación mientras se documenta

PetMatch no es actualmente:

```text
tienda
inventario
plataforma de adopción
chat
red social completa
sistema de pagos
sistema de geolocalización
```

El objetivo del libro es explicar el código existente, no rediseñar silenciosamente el dominio.

---

# 68. Error: inventar un endpoint porque “debería existir”

Un caso real está en `SupportApplicationRestController`.

Después de crear una application se genera un `Location` individual, pero actualmente no existe un:

```text
GET /api/v1/support-applications/{id}
```

Ese endpoint no forma parte de la API actual, aunque su ausencia haga el diseño menos simétrico.

La asimetría se documenta y, si se desea, se trata como evolución futura.

---

# 69. Error: asumir que README reemplaza el código

El README ayuda a orientarse, pero los ejemplos y explicaciones técnicas se contrastan con:

```text
rama main
→ código actual
```

Si una descripción histórica contradice una clase real, se debe volver al código.

---

# 70. Error: no leer los nombres de métodos Repository

Métodos como:

```text
findByIdAndOwnerId
findByStatusAndServiceDateAfterOrderByServiceDateAsc
existsByApplicantIdAndSupportRequestId
```

contienen información importante sobre intención de la consulta.

Leer solo:

```text
repository.find...
```

sin interpretar el nombre hace perder parte del diseño.

---

# 71. Error: no distinguir fallo técnico de regla de dominio

Ejemplos:

```text
JSON no convertible
→ problema de entrada HTTP

request no OPEN
→ regla/estado del dominio

DB_URL ausente
→ configuración/infraestructura

usuario sin Basic
→ autenticación
```

Clasificar correctamente el tipo de error ayuda a buscar en la capa correcta.

---

# 72. Error: depurar empezando en una capa aleatoria

Usa el recorrido:

```text
request
→ Security
→ Controller
→ DTO/validation
→ Service
→ Repository
→ DB
```

Pregunta dónde deja de cumplirse la expectativa.

No empieces cambiando cinco capas a la vez.

---

# 73. Error: cambiar Controller, Service y Repository simultáneamente para “probar”

Ese enfoque dificulta identificar qué cambio arregló o rompió el comportamiento.

Haz cambios pequeños y verificables.

Esto conecta directamente con Git:

```text
cambio pequeño
→ prueba
→ commit coherente
```

El siguiente capítulo desarrolla esa disciplina.

---

# 74. Error: tener commits gigantes con temas no relacionados

Un commit que mezcla:

```text
seguridad
CSS
renombrado
DB
feature nueva
```

es difícil de revisar y revertir.

Un commit debería contar una historia técnica coherente.

Relacionado: [36 — Git, GitHub y versionado](../07-referencia/36-git-github-y-versionado.md).

---

# 75. Error: subir artefactos generados

El repositorio ignora:

```text
target/
petmatch-community.zip
```

porque son artefactos generados/reconstruibles.

Versionar archivos compilados normalmente añade ruido y conflictos innecesarios.

---

# 76. Error: subir configuración local del IDE

`.gitignore` excluye configuraciones de:

```text
IntelliJ
STS
NetBeans
VS Code
```

El proyecto debe conservar configuración compartida relevante, no preferencias personales innecesarias.

---

# 77. Error: editar `mvnw` y `mvnw.cmd` ignorando finales de línea

`.gitattributes` define:

```text
/mvnw text eol=lf
*.cmd text eol=crlf
```

Esto ayuda a mantener comportamiento consistente entre plataformas.

No elimines reglas de atributos sin entender por qué están allí.

---

# 78. Error: suponer que `.gitignore` borra archivos ya versionados

Si un archivo ya está tracked, añadir una regla a `.gitignore` no lo saca automáticamente del historial ni del índice.

Git distingue:

```text
tracked
untracked
ignored
```

Este concepto se desarrolla en el capítulo 36.

---

# 79. Diagnóstico rápido por síntoma

| Síntoma | Primera zona a revisar |
|---|---|
| 401 en `/api/**` | HTTP Basic / SecurityFilterChain |
| Pet ajena visible/modificable | ownership en Service/Repository |
| 400 con `Validation failed` | API DTO constraints |
| 400 `Invalid request body` | JSON/type conversion |
| 404 para id ajeno | política ownership/visibility |
| 409 en operación | estado/regla de negocio |
| LazyInitializationException | frontera transaccional/fetch |
| cambio de Entity no persiste | managed state/transacción |
| dos accepted posibles | locking/constraints/transaction |
| tests no levantan | DB env/ApplicationContext |
| password no autentica | encoder/credencial/email |
| CSS no carga | CDN/conectividad/template |
| archivo local aparece para commit | `.gitignore` |

---

# 80. Método de depuración recomendado

Cuando algo falla:

```text
1. reproduce el problema
2. identifica la request/caso de uso
3. localiza Controller
4. identifica DTO y validation
5. entra al Service
6. identifica ownership/estado
7. revisa query Repository
8. revisa configuración si corresponde
9. ejecuta prueba relevante
10. realiza el cambio mínimo
11. vuelve a probar
12. crea commit coherente
```

---

# 81. Caso guiado — “puedo editar la Pet de otro usuario”

No empieces en CSS.

Pregunta:

```text
¿qué método carga la Pet para update?
```

Debe aparecer una ruta como:

```text
PetService.findOwnedPet
→ findByIdAndOwnerId
```

Si alguien reemplazara esa lógica por un simple `findById`, la revisión debería detectar la pérdida de ownership.

---

# 82. Caso guiado — “POST API devuelve 403 por CSRF”

En el estado actual, `/api/**` usa una chain con CSRF deshabilitado.

Revisar:

```text
¿la URL realmente empieza por /api/?
¿la chain API sigue haciendo match?
¿se alteró el orden?
```

No soluciones desactivando CSRF globalmente sin comprender por qué dejó de entrar a la chain correcta.

---

# 83. Caso guiado — “el test de integración no inicia”

Antes de culpar a JUnit, revisa:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
MySQL disponible
ApplicationContext
```

No existe actualmente un H2/Testcontainers que elimine esa dependencia del entorno.

---

# 84. Caso guiado — “cambié status pero no veo `save()`”

Pregunta:

```text
¿la Entity fue cargada dentro de @Transactional?
¿sigue managed?
```

Si sí, dirty checking puede ser la razón por la que el cambio se sincroniza.

No agregues `save()` mecánicamente antes de comprender el ciclo de vida JPA.

---

# 85. Caso guiado — “quiero agregar JWT porque REST usa Basic”

Primero separa:

```text
problema actual
```

de:

```text
tecnología interesante
```

PetMatch actual usa Basic deliberadamente para el MVP educativo.

JWT sería una evolución que afectaría SecurityConfig, autenticación, testing, expiración/revocación y manejo del cliente.

No es una corrección automática.

---

# 86. Checklist antes de entregar una funcionalidad

Comprueba:

- ¿la lógica quedó en la capa correcta?;
- ¿el current user viene de Authentication?;
- ¿los ids controlados por cliente pasan por ownership?;
- ¿el DTO expone solo lo necesario?;
- ¿Bean Validation cubre estructura?;
- ¿Service cubre reglas?;
- ¿el estado anterior es válido?;
- ¿la operación necesita transacción?;
- ¿existe riesgo concurrente?;
- ¿las relaciones JPA necesarias se cargan correctamente?;
- ¿el error HTTP representa bien el fallo?;
- ¿hay test del comportamiento más importante?;
- ¿no se introdujeron secretos/artefactos al commit?;
- ¿el commit cuenta una historia coherente?.

---

# 87. 🧪 Comprueba que entendiste

1. ¿Por qué `@Valid` no reemplaza ownership?
2. ¿Por qué un usuario autenticado no puede usar cualquier `petId`?
3. ¿Dónde deben vivir self-apply y duplicate apply?
4. ¿Por qué Entity no debe ser automáticamente el DTO REST?
5. ¿Qué diferencia hay entre 401 y 409?
6. ¿`STATELESS` elimina la base de datos?
7. ¿HTTP Basic cifra el transporte?
8. ¿por qué no se desactiva CSRF globalmente?
9. ¿qué diferencia hay entre Mockito y MockMvc?
10. ¿`contextLoads()` prueba el flujo completo?
11. ¿el proyecto usa H2 actualmente?
12. ¿el proyecto usa Testcontainers actualmente?
13. ¿`ddl-auto: update` equivale a Flyway?
14. ¿`.env` se debe commitear?
15. ¿Spring Boot carga necesariamente `.env` por sí solo en este proyecto?
16. ¿qué hace dirty checking?
17. ¿por qué existe `PESSIMISTIC_WRITE` en accept?
18. ¿la suite actual prueba dos accepts simultáneos reales?
19. ¿existe File & Image Upload implementado?
20. ¿existe GET individual de SupportApplication REST actualmente?

### Respuestas esperadas

1. Porque validation estructural no conoce la relación entre usuario y recurso.
2. Porque se debe verificar ownership en backend.
3. En el Service/caso de uso.
4. Porque persistencia y contrato externo son responsabilidades distintas.
5. 401 trata autenticación; 409 conflicto de regla/estado.
6. No.
7. No; se necesita HTTPS para cifrar transporte.
8. Porque la web usa sesión y conserva la protección CSRF estándar.
9. Mockito simula objetos; MockMvc prueba requests/responses del stack MVC.
10. No.
11. No está cubierto.
12. No está cubierto.
13. No.
14. No; está ignorado.
15. No debe asumirse; las variables deben estar disponibles en el entorno.
16. Detecta cambios de Entities managed dentro de una transacción.
17. Para coordinar la decisión concurrente sobre una request.
18. No.
19. No.
20. No.

---

# 88. ✅ Qué debes recordar

Los errores más importantes de este capítulo pueden resumirse en diez reglas:

```text
1. no pongas negocio en Controller
2. no confíes en ids del cliente sin ownership
3. autenticación no sustituye autorización
4. validation no sustituye reglas de Service
5. Entity no es DTO
6. transacción y estado managed explican dirty checking
7. concurrencia requiere diseño explícito
8. Security web y API no son la misma política
9. tests prueban solo lo que realmente ejecutan
10. documentación debe describir código existente, no deseos
```

Y una regla adicional de trabajo profesional:

```text
cambio pequeño
→ prueba
→ commit coherente
```

Ese es el puente hacia el bloque de referencia.

---

# 🔗 Continúa con

El siguiente capítulo explica cómo Git y GitHub permiten conservar, compartir y revisar la evolución de este proyecto sin confundir el código actual con archivos locales o cambios todavía no registrados.

**[Capítulo 36 — Git, GitHub y versionado →](../07-referencia/36-git-github-y-versionado.md)**

---

[← Capítulo 34 — Buenas prácticas y decisiones](34-buenas-practicas-y-decisiones.md) · [Índice del bloque](README.md) · [Índice general](../README.md) · [Siguiente → Capítulo 36](../07-referencia/36-git-github-y-versionado.md)
