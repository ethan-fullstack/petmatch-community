# Construye tu propia aplicación con Spring Boot

Esta ruta reúne todo el libro en una sola pregunta:

> **¿Puedes pasar de una idea propia a una aplicación Spring Boot funcional, explicable y comprobable sin copiar PetMatch ni `BookShelf`?**

PetMatch seguirá siendo la aplicación de referencia y `BookShelf` seguirá siendo el ejemplo de transferencia utilizado en las prácticas. Pero en esta ruta **el dominio lo eliges tú**.

La meta no es terminar con las mismas clases cambiando nombres.

La meta es tomar decisiones propias sobre:

```text
problema
→ dominio
→ casos de uso
→ persistencia
→ reglas
→ interfaz web
→ seguridad
→ API si aplica
→ pruebas
→ Git
```

---

## Cómo usar esta ruta

No reemplaza los capítulos 01–39.

Úsala de dos maneras:

### Si todavía estás aprendiendo

Avanza por bloques del libro y vuelve aquí al terminar cada uno.

### Si ya terminaste el libro

Intenta construir una aplicación nueva siguiendo únicamente esta ruta.

Cuando una decisión no sea clara, regresa al capítulo correspondiente.

La prueba final es sencilla:

> **si necesitas copiar una clase completa de PetMatch para continuar, todavía falta comprender la responsabilidad que esa clase representa.**

---

# 1. Elige un problema antes de elegir anotaciones

No empieces creando:

```text
Controller
Service
Repository
Entity
```

Empieza escribiendo el problema.

Ejemplo de formato:

```text
La aplicación permite a ______________________
realizar ______________________
para resolver ______________________.
```

Un ejemplo distinto de PetMatch y `BookShelf` podría ser:

```text
La aplicación permite a un pequeño taller
registrar equipos recibidos para reparación,
seguir su estado y registrar su entrega al cliente.
```

Eso ya sugiere conceptos del dominio.

---

# 2. Describe el dominio con palabras, no con clases

Antes de Java identifica:

```text
actores
objetos importantes
acciones
reglas
estados
relaciones
```

Ejemplo conceptual:

```text
Actor
→ técnico

Objeto principal
→ equipo recibido

Objeto relacionado
→ cliente

Acciones
→ registrar
→ diagnosticar
→ reparar
→ entregar

Estados
→ RECEIVED
→ IN_REPAIR
→ READY
→ DELIVERED
```

Todavía no escribas `@Entity`.

Primero verifica que el modelo tenga sentido como problema real.

---

# 3. Define un alcance mínimo

Tu primera versión debe poder explicarse en pocas frases.

Un alcance razonable suele tener:

```text
1–2 actores principales
3–5 conceptos persistentes
1 flujo central
algunas reglas de negocio
login/registro si el dominio lo necesita
una interfaz web
pruebas del flujo importante
```

Evita comenzar con:

```text
microservicios
chat
notificaciones en tiempo real
pagos
Kafka
Redis
OAuth2
JWT
Docker obligatorio
arquitectura distribuida
```

No porque esas tecnologías sean incorrectas, sino porque pueden ocultar si realmente comprendes Spring Boot, MVC, JPA y Security.

---

# 4. Escribe los casos de uso antes de crear rutas

Ejemplo:

```text
CU01 — registrar cliente
CU02 — registrar equipo
CU03 — listar equipos
CU04 — ver detalle
CU05 — cambiar estado del equipo
CU06 — consultar solo equipos propios/asignados
```

Cada caso de uso debería responder:

```text
¿quién lo ejecuta?
¿qué datos recibe?
¿qué regla debe cumplir?
¿qué cambia?
¿qué resultado espera?
```

Esto te ayudará después a decidir qué pertenece a Controller, Service y Repository.

---

# 5. Decide qué NO hará la primera versión

Escribe una lista corta.

Ejemplo:

```text
No habrá pagos.
No habrá carga de archivos.
No habrá notificaciones por correo.
No habrá app móvil.
No habrá API pública en la primera entrega.
```

Definir límites evita que el proyecto crezca antes de tener una base sólida.

---

# 6. Crea el proyecto con Spring Initializr

Para reproducir el entorno técnico estudiado en este libro puedes usar como referencia:

```text
Project: Maven
Language: Java
Spring Boot: 4.1.1
Packaging: Jar
Java: 21
```

Elige tus propios metadatos.

Ejemplo:

```text
Group: com.example
Artifact: repairdesk
Name: RepairDesk
Package name: com.example.repairdesk
```

No uses:

```text
com.petmatch.community
com.example.bookshelf
```

si tu dominio es otro.

---

# 7. Empieza con las dependencias que necesitas ahora

Para la primera comprobación basta con una capacidad web.

Dependencia inicial:

```text
Spring Web MVC
```

Después agregarás persistencia, Thymeleaf, Validation y Security cuando el proyecto llegue a esas necesidades.

Esto te obliga a reconocer **por qué** aparece cada dependencia.

---

# 8. Reconoce lo que generó Initializr

Antes de crear paquetes adicionales identifica:

```text
pom.xml
mvnw
mvnw.cmd
.mvn/
src/main/java/...Application.java
src/main/resources/
src/test/java/
```

Debes saber distinguir:

```text
estructura generada
≠
estructura creada por ti
```

---

# 9. Comprueba Java

```bash
java -version
```

La terminal debe estar usando un JDK compatible con el proyecto.

Si estudias con la misma base del libro, espera Java 21.

No intentes resolver errores de compilación Spring antes de comprobar el JDK.

---

# 10. Ejecuta el proyecto vacío

Linux/macOS/WSL:

```bash
./mvnw test
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Este es tu primer checkpoint:

```text
proyecto generado
→ compila
→ test base ejecuta
→ aplicación arranca
```

No avances con veinte clases si todavía falla esta base.

---

# 11. Inicializa Git

Si el proyecto todavía no es un repositorio Git:

```bash
git init
```

Comprueba:

```bash
git status
```

Después:

```bash
git add .
git commit -m "chore: bootstrap Spring Boot project"
```

Si Git indica que falta configurar tu identidad, configura el nombre y correo que correspondan a tu entorno antes del commit.

La idea importante es:

```text
cambio pequeño
→ comprobar
→ revisar diff
→ commit coherente
```

---

# 12. Crea una primera cadena Controller → Service

Antes de persistencia construye una interacción mínima.

Paquetes sugeridos:

```text
controller/
service/
```

No necesitas crear todos los paquetes posibles todavía.

Ejemplo conceptual:

```java
@Service
public class EquipmentService {

    public String getWelcomeMessage() {
        return "Repair desk ready";
    }
}
```

Y:

```java
@RestController
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(
        EquipmentService equipmentService
    ) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/status-message")
    public String statusMessage() {
        return equipmentService.getWelcomeMessage();
    }
}
```

No copies estos nombres si tu dominio es otro.

Lo que debes transferir es:

```text
Controller
→ depende de Service
→ constructor injection
→ Spring resuelve el Bean
```

---

# 13. Comprueba Dependency Injection

Ejecuta la aplicación y accede al endpoint.

Después responde:

```text
¿quién creó EquipmentService?
¿por qué Controller no usa new EquipmentService()?
¿cómo encontró Spring ambas clases?
```

Si no puedes responder, revisa los capítulos 03 y 08.

---

# 14. Define tus Entities desde el dominio

Ahora vuelve a la lista de conceptos inicial.

Pregunta para cada uno:

```text
¿necesita identidad persistente?
¿debe sobrevivir al reinicio de la aplicación?
¿tiene relaciones con otros conceptos?
```

Solo entonces crea Entities.

Ejemplo conceptual:

```text
Customer
Equipment
RepairOrder
```

No conviertas cada palabra del problema en Entity.

---

# 15. Dibuja primero las relaciones

Ejemplo:

```text
Customer 1 ─── N Equipment
Equipment 1 ─── N RepairOrder
```

Para cada relación responde:

```text
¿cuál es la cardinalidad?
¿dónde estará la foreign key?
¿qué lado necesita navegar al otro?
¿la relación debe ser LAZY?
¿necesitas cascade realmente?
```

Cardinalidad y cascade son decisiones diferentes.

---

# 16. Agrega persistencia

Cuando el dominio ya la necesite, agrega las dependencias correspondientes a:

```text
Spring Data JPA
MySQL Driver
```

Después configura el datasource.

Ejemplo:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
```

Este libro utiliza `ddl-auto: update` como estrategia didáctica de desarrollo.

No lo confundas con una herramienta de migraciones versionadas.

---

# 17. Crea la base de datos

Ejemplo genérico:

```sql
CREATE DATABASE repairdesk
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

Adapta el nombre a tu proyecto.

Variables Linux/macOS/WSL:

```bash
export DB_URL="jdbc:mysql://localhost:3306/repairdesk"
export DB_USERNAME="root"
export DB_PASSWORD="TU_PASSWORD"
```

PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/repairdesk"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="TU_PASSWORD"
```

No escribas la contraseña directamente en `application.yaml` para compartirla en Git.

---

# 18. Crea Repositories para preguntas de persistencia

Un Repository debe responder preguntas sobre datos.

Ejemplos conceptuales:

```text
findByEmailIgnoreCase
existsBySerialNumber
findByStatusOrderByCreatedAtDesc
findByIdAndOwnerId
```

No debe decidir reglas como:

```text
“el equipo puede entregarse”
“el usuario tiene permitido cambiar este estado”
```

Eso corresponde al caso de uso.

---

# 19. Lleva las reglas al Service

Para cada caso de uso escribe las guardas.

Ejemplo:

```text
completeRepair(orderId)

1. order debe existir
2. usuario debe poder operar el recurso
3. estado debe ser IN_REPAIR
4. cambia a READY
```

Ese algoritmo pertenece naturalmente al Service.

El Controller debería coordinar HTTP, no reimplementar estas reglas.

---

# 20. Usa transacciones cuando un caso de uso sea una unidad

Pregunta:

> ¿qué cambios deben confirmarse juntos o fallar juntos?

Ejemplo:

```text
cerrar reparación
→ RepairOrder cambia estado
→ Equipment cambia disponibilidad
→ se registra fecha de finalización
```

Si esos cambios forman una unidad de negocio, el Service puede necesitar:

```java
@Transactional
```

No agregues `@Transactional` a todas las clases por costumbre.

---

# 21. Modela estados cuando el dominio los tenga

Si un recurso cambia por etapas, usa un modelo explícito.

Ejemplo:

```java
public enum RepairStatus {
    RECEIVED,
    IN_REPAIR,
    READY,
    DELIVERED
}
```

Después define transiciones válidas.

```text
RECEIVED → IN_REPAIR
IN_REPAIR → READY
READY → DELIVERED
```

No basta con tener el `enum`: el Service debe proteger las transiciones.

---

# 22. Pregunta si existe concurrencia real

No todo proyecto necesita locking.

Busca un recurso compartido sobre el cual dos operaciones simultáneas puedan competir.

Ejemplos:

```text
última unidad disponible
último cupo de una cita
una única aceptación posible
un mismo recurso que solo puede asignarse una vez
```

Si no existe esa competencia, no copies `PESSIMISTIC_WRITE` solo porque PetMatch lo utiliza.

Si sí existe, revisa el capítulo 16 y justifica qué recurso debe bloquearse.

---

# 23. Diseña el fetch plan

Con:

```yaml
open-in-view: false
```

las vistas y mappers deben recibir las relaciones que realmente necesitan.

Pregunta por cada consulta:

```text
¿qué Entity principal devuelve?
¿qué asociaciones leerá la capa siguiente?
¿cuáles siguen LAZY?
¿necesito @EntityGraph en esta consulta?
```

No resuelvas un `LazyInitializationException` cambiando todas las relaciones a EAGER.

---

# 24. Checkpoint de persistencia

Antes de construir la interfaz debes poder explicar:

```text
Entity
→ tabla

@ManyToOne
→ foreign key

Repository
→ pregunta de persistencia

Service
→ regla/caso de uso

@Transactional
→ límite de unidad de trabajo

LAZY
→ carga diferida

EntityGraph
→ fetch plan de una consulta concreta
```

También debes poder mostrar datos reales en MySQL.

---

# 25. Diseña primero las rutas web

Antes del Controller escribe una tabla.

Ejemplo:

| Método | Ruta | Propósito |
|---|---|---|
| GET | `/equipment` | listar |
| GET | `/equipment/new` | formulario |
| POST | `/equipment` | crear |
| GET | `/equipment/{id}` | detalle |
| GET | `/equipment/{id}/edit` | editar |
| POST | `/equipment/{id}` | actualizar |

Adapta las rutas a tu dominio.

La tabla te obliga a separar:

```text
mostrar una página
≠
procesar una acción
```

---

# 26. Agrega Thymeleaf y Validation

Cuando aparezcan formularios agrega las capacidades correspondientes:

```text
Spring Thymeleaf
Bean Validation
```

No necesitas un frontend SPA para demostrar MVC server-side.

---

# 27. Crea Form DTO

Evita usar directamente una Entity como formulario universal.

Pregunta:

```text
¿qué campos puede controlar realmente el usuario?
```

Ejemplo:

```java
public class EquipmentForm {

    @NotBlank
    @Size(max = 120)
    private String brand;

    @NotBlank
    @Size(max = 120)
    private String model;

    @NotBlank
    @Size(max = 80)
    private String serialNumber;

    // getters y setters
}
```

No incluyas automáticamente:

```text
id
ownerId
status interno
createdAt
campos de auditoría
```

si el usuario no debe controlarlos.

---

# 28. Construye el flujo MVC

Tu recorrido debe poder explicarse así:

```text
Browser
→ Controller
→ Form DTO
→ @Valid
→ BindingResult
→ Service
→ Repository
→ DB
→ redirect
→ GET
→ Model
→ Thymeleaf
→ HTML
```

No memorices la cadena: comprueba cada paso con tu código.

---

# 29. Distingue render de redirect

Render:

```java
return "equipment/form";
```

indica un template.

Redirect:

```java
return "redirect:/equipment";
```

indica una nueva request del navegador.

Después de una creación válida suele ser útil aplicar Post/Redirect/Get.

---

# 30. Separa validación estructural de regla de negocio

Bean Validation puede decidir:

```text
campo obligatorio
longitud
formato email
rango numérico
```

Service puede decidir:

```text
serialNumber ya existe
estado no permite operación
recurso no pertenece al usuario
```

No intentes expresar toda regla del dominio mediante anotaciones del DTO.

---

# 31. Comprueba la interfaz antes de Security

Prueba manualmente:

```text
lista vacía
crear válido
crear inválido
ver detalle
editar
error de regla de negocio
```

Si el CRUD o flujo base está roto, agregar autenticación hará más difícil diagnosticarlo.

---

# 32. Decide qué identidad necesita el sistema

Si tu aplicación necesita cuentas, define:

```text
qué representa un usuario
qué dato identifica el login
qué roles existen
qué recursos tienen owner
```

No agregues login solo porque todas las demos lo tienen.

Pero si el dominio depende de “mis recursos”, necesitas una identidad confiable.

---

# 33. Agrega Spring Security

Construye progresivamente:

```text
User/Account Entity
→ Repository
→ RegistrationForm
→ PasswordEncoder
→ registro
→ UserDetailsService
→ SecurityFilterChain
→ form login
→ Authentication
```

Nunca guardes la contraseña en texto plano.

---

# 34. Password hashing pertenece a infraestructura de seguridad

La Entity puede almacenar:

```text
passwordHash
```

pero no debería ejecutar por sí misma el algoritmo de hashing.

El Service de registro utiliza:

```java
PasswordEncoder
```

antes de persistir.

---

# 35. Autenticación y autorización son distintas

Autenticación:

```text
¿quién eres?
```

Autorización:

```text
¿puedes hacer esto?
```

Un usuario autenticado no debe adquirir acceso automático a todos los registros de la tabla.

---

# 36. Implementa ownership en backend

Si una operación trabaja con un recurso del usuario actual, evita recibir desde el formulario:

```text
ownerId
userId
borrowerId
```

como fuente de verdad.

Usa:

```text
Authentication
→ current user
→ id persistente
→ consulta owned
```

Ejemplo conceptual:

```text
findByIdAndOwnerId(resourceId, currentUserId)
```

La UI puede ocultar botones; la protección real sigue en backend.

---

# 37. Conserva CSRF para la web basada en sesión

La interfaz web con form login trabaja con sesión.

No copies una instrucción genérica como:

```java
csrf.disable()
```

en toda la aplicación.

La política REST estudiada más adelante es diferente.

---

# 38. Checkpoint de seguridad

Debes poder demostrar:

```text
registro
→ hash persistido

login incorrecto
→ falla

login correcto
→ sesión autenticada

ruta protegida
→ exige login

rol insuficiente
→ no accede

recurso ajeno
→ Service/Repository lo bloquea
```

---

# 39. Decide si tu aplicación necesita API REST

No toda aplicación MVC necesita una API paralela.

Agrégala si:

```text
la actividad la exige
otro cliente debe consumir el sistema
quieres practicar el segundo contrato de transporte del libro
```

Si tu alcance técnico debe equivaler al recorrido completo de PetMatch, construye también esta interfaz.

---

# 40. Reutiliza los mismos Services

La arquitectura debería verse así:

```text
MVC Controller ─────┐
                    ↓
                 Service
                    ↑
REST Controller ────┘
```

No así:

```text
MVC Service
REST Service
```

si ambos representan exactamente el mismo caso de uso.

---

# 41. Diseña el contrato REST

Para cada endpoint escribe:

```text
método HTTP
ruta
request DTO
response DTO
status de éxito
posibles errores
```

Ejemplo:

```text
POST /api/v1/equipment
Request: EquipmentApiRequest
Success: 201 Created
Location: /api/v1/equipment/{id}
Body: EquipmentApiResponse
```

---

# 42. Separa API DTO de Entity y Form DTO

```text
Entity
→ persistencia

Form DTO
→ interacción MVC

API Request DTO
→ contrato JSON de entrada

API Response DTO
→ contrato JSON de salida
```

Pueden compartir campos.

No son la misma responsabilidad.

---

# 43. Crea un mapper pequeño y explícito

Puede transformar:

```text
API Request DTO
→ Form DTO o comando esperado por Service
```

y:

```text
Entity
→ API Response DTO
```

No pongas reglas de negocio en el mapper.

---

# 44. Usa semántica HTTP intencional

Ejemplos:

```text
GET correcto
→ 200

recurso creado
→ 201 + Location

acción correcta sin body
→ 204

request inválido
→ 400

sin autenticación
→ 401

recurso inexistente/no visible
→ 404

conflicto de estado/regla
→ 409
```

No devuelvas `200` para todo.

---

# 45. Separa seguridad web y REST cuando la política sea distinta

El patrón estudiado en el libro es:

```text
/api/**
→ HTTP Basic
→ STATELESS
→ CSRF disabled en esa chain

web
→ form login
→ sesión
→ CSRF web
```

Esto es una decisión didáctica del proyecto estudiado.

No significa que toda API real deba usar HTTP Basic.

---

# 46. Traduce errores REST con intención

Un `@RestControllerAdvice` puede traducir excepciones conocidas a `ProblemDetail`.

Ejemplo de clasificación:

```text
JSON ilegible
→ 400

Bean Validation
→ 400 + errors

ResourceNotFoundException
→ 404

BusinessRuleException
→ 409
```

No agregues un handler genérico de `Exception.class` solo para ocultar cualquier fallo.

---

# 47. Comprueba la API manualmente

Usa un cliente HTTP y verifica al menos:

```text
sin credenciales
con credenciales
GET exitoso
POST exitoso
validación inválida
recurso inexistente
conflicto de negocio
ownership
```

Con `curl` puedes observar status, headers y body.

---

# 48. Diseña tests desde comportamientos

No empieces por:

```text
“necesito Mockito”
```

Empieza por:

```text
¿qué regla quiero demostrar?
```

Después elige el nivel de prueba.

---

# 49. Escribe unit tests de reglas de Service

Usa:

```text
Service real
+
colaboradores mock
```

Un unit test puede demostrar:

```text
estado inválido
→ excepción

regla válida
→ interacción esperada
```

No demuestra SQL, DB ni SecurityFilterChain.

---

# 50. Escribe integración donde necesites infraestructura real

Cuando la pregunta requiera:

```text
Spring
JPA
Repositories
DB
transacción
```

usa un test de integración adecuado.

Si tu configuración no incluye H2/Testcontainers/perfil de test, recuerda que `@SpringBootTest` puede requerir un datasource real accesible.

---

# 51. Usa `MockMvc` para la frontera HTTP de Spring MVC

Una integración REST con `MockMvc` puede incluir:

```text
Security
routing
JSON
validation
Controller
Service
Repository
DB
```

No equivale automáticamente a un navegador o sistema end-to-end completo.

Describe siempre la frontera real.

---

# 52. Define una suite mínima defendible

Como mínimo, para una aplicación académica de este alcance intenta tener:

```text
1+ unit test de una regla importante
1 integración de un flujo de negocio
1 integración HTTP de seguridad/éxito/error si tienes REST
contextLoads como smoke test del contexto
```

La cantidad exacta depende del proyecto.

La calidad se evalúa por la evidencia, no por alcanzar un número arbitrario de archivos.

---

# 53. Ejecuta la suite completa

```bash
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd clean test
```

No digas:

```text
“los tests están bien”
```

si no los ejecutaste.

Lee el resultado de Maven.

---

# 54. Diagnostica por capas

Cuando algo falle, pregunta primero dónde ocurrió.

| Síntoma | Primera capa a revisar |
|---|---|
| aplicación no arranca | configuración / Beans |
| DB connection refused | infraestructura / datasource |
| `LazyInitializationException` | fetch plan / transacción |
| formulario vuelve con errores | binding / validation |
| 401 | autenticación / SecurityFilterChain |
| 404 owned | Service / ownership query |
| 409 | regla de negocio / estado |
| JSON 400 | deserialización / validation |
| Mockito `Wanted but not invoked` | escenario/interacción del unit test |

No modifiques cinco capas a la vez.

---

# 55. Usa Git durante todo el proyecto

Un historial útil puede reflejar avances como:

```text
chore: bootstrap Spring Boot project
feat: add core domain model
feat: add persistence repositories
feat: implement main business flow
feat: add web forms
feat: add authentication and ownership
feat: expose REST API
 test: cover main business rules
```

No copies estos mensajes literalmente si no describen tu cambio.

Antes de cada commit:

```bash
git status
git diff
```

Después de los tests:

```bash
git add .
git commit -m "..."
```

---

# 56. No versiones secretos ni artefactos generados

Revisa `.gitignore`.

Normalmente no deberían formar parte del historial:

```text
target/
.env
credenciales
archivos locales del IDE
```

Un `.gitignore` no elimina un secreto que ya fue committeado.

---

# 57. Construye tu propio mapa concepto → clase

Al terminar, deberías poder llenar una tabla como:

| Concepto | Tu clase/archivo | Por qué vive allí |
|---|---|---|
| arranque | `...Application` | punto de entrada |
| autenticación | `...UserDetailsService` | adapta usuario a Spring Security |
| ownership | `...Service` + Repository | protege recurso individual |
| transacción | método de Service | caso de uso atómico |
| validación | Form/API DTO | estructura de entrada |
| REST error | `ApiExceptionHandler` | contrato HTTP de error |
| test unitario | `...ServiceTests` | regla aislada |

Si no puedes completar “por qué vive allí”, vuelve a revisar la arquitectura.

---

# 58. Construye un glosario de tu propio proyecto

Elige al menos diez términos que realmente utilices.

Ejemplo:

```text
Bean
Entity
Repository
DTO
Authentication
ownership
transaction
dirty checking
ProblemDetail
MockMvc
```

Para cada uno escribe:

```text
qué significa
cómo se usa
qué clase propia lo demuestra
```

Eso convierte vocabulario en comprensión aplicada.

---

# 59. Separa implementado de futuro

Crea dos listas para tu sustentación o documentación.

## Implementado

Solo cosas demostrables en código.

## Posibles evoluciones

Ideas que resolverían una necesidad futura.

Ejemplo:

```text
Implementado
→ MySQL + ddl-auto update

Posible evolución
→ Flyway/Liquibase si necesito migraciones versionadas
```

Nunca presentes una tecnología futura como si ya fuera parte de la aplicación.

---

# 60. Evalúa alternativas mediante necesidad y trade-off

Para cualquier herramienta nueva completa:

```text
problema
→ evidencia
→ solución actual
→ limitación actual
→ alternativa
→ beneficio
→ coste
→ cómo se probaría
```

Ejemplo:

```text
problema
→ tests dependen de MySQL local

alternativa
→ Testcontainers

beneficio
→ entorno reproducible

coste
→ runtime de contenedores + más infraestructura
```

La herramienta viene después del problema.

---

# 61. Recorrido completo que debes poder explicar

Tu aplicación debería poder narrarse así:

```text
usuario
↓
request HTTP
↓
SecurityFilterChain
↓
Controller
↓
DTO de entrada
↓
validation
↓
Service
↓
ownership + reglas + estados
↓
@Transactional
↓
Repository
↓
JPA/Hibernate
↓
MySQL
↓
resultado
↓
View o API Response DTO
↓
HTML o JSON
```

No todas las operaciones pasan por absolutamente todas las piezas.

El objetivo es saber cuándo aparece cada una.

---

# 62. Orden recomendado de construcción

Usa esta secuencia como guía práctica:

```text
01 problema y alcance
02 casos de uso
03 Initializr
04 ejecutar proyecto vacío
05 Git inicial
06 Controller → Service mínimo
07 dominio
08 JPA + DB
09 Repositories
10 reglas en Services
11 transacciones/estados
12 fetch plan
13 MVC + Thymeleaf
14 Form DTO + Validation
15 registro/login
16 roles + ownership
17 REST si aplica
18 ProblemDetail
19 unit tests
20 integration tests
21 diagnóstico
22 documentación conceptual
23 revisión Git
24 sustentación
```

No es obligatorio que todos los dominios necesiten exactamente las mismas piezas.

---

# 63. Definition of Done técnica

Antes de considerar terminada tu primera versión, revisa:

- [ ] el problema puede explicarse en menos de un minuto;
- [ ] los casos de uso principales están definidos;
- [ ] Java y Maven Wrapper funcionan;
- [ ] el proyecto arranca desde una terminal limpia;
- [ ] las credenciales no están hardcodeadas en el repositorio;
- [ ] las Entities representan conceptos del dominio;
- [ ] las relaciones JPA pueden justificarse;
- [ ] los Repositories responden preguntas de persistencia;
- [ ] las reglas importantes viven en Services;
- [ ] los cambios atómicos tienen un límite transaccional razonable;
- [ ] los estados inválidos son rechazados;
- [ ] locking solo existe si hay una carrera que lo justifique;
- [ ] el fetch plan evita depender de Open Session in View;
- [ ] la interfaz MVC utiliza Form DTO cuando corresponde;
- [ ] la validación estructural está separada de negocio;
- [ ] las contraseñas se almacenan mediante hash;
- [ ] las rutas protegidas requieren autenticación;
- [ ] ownership se verifica en backend;
- [ ] la API, si existe, reutiliza Services;
- [ ] la API no expone Entities directamente;
- [ ] los status HTTP son intencionales;
- [ ] los errores REST relevantes tienen contrato coherente;
- [ ] existe evidencia unitaria de una regla importante;
- [ ] existe integración para un flujo relevante;
- [ ] puedes explicar qué NO prueban tus tests;
- [ ] `./mvnw clean test` o su equivalente Windows se ejecuta con éxito en el entorno preparado;
- [ ] `git status` no muestra secretos ni artefactos que no deban versionarse;
- [ ] los commits representan cambios coherentes;
- [ ] puedes diferenciar lo implementado de posibles evoluciones.

---

# 64. Definition of Done pedagógica

También debes poder responder sin leer código:

1. ¿qué problema resuelve tu aplicación?
2. ¿cuál es el flujo principal?
3. ¿por qué elegiste esas Entities?
4. ¿dónde viven las reglas de negocio?
5. ¿qué hace una transacción importante?
6. ¿qué recurso protege ownership?
7. ¿qué diferencia hay entre autenticación y autorización?
8. ¿por qué no usas Entity directamente como formulario/API DTO?
9. ¿qué prueba tu unit test?
10. ¿qué prueba tu integración?
11. ¿qué falla antes de llegar al Controller cuando obtienes 401?
12. ¿qué tecnología decidiste no agregar y por qué?

Si una respuesta es:

> “porque PetMatch lo hace así”

vuelve al capítulo correspondiente y busca la necesidad que originó esa decisión.

---

# 65. Señales de que estás copiando en vez de transferir

Revisa tu proyecto si aparecen clases como:

```text
Pet
SupportRequest
SupportApplication
Book
Loan
```

sin que realmente pertenezcan a tu dominio.

También es una señal si agregaste:

```text
PESSIMISTIC_WRITE
EntityGraph
ADMIN
REST
ProblemDetail
```

pero no puedes describir qué problema propio resuelven.

La coincidencia técnica puede ser correcta.

Lo que debe cambiar es la justificación y el dominio.

---

# 66. Señales de una transferencia correcta

Vas por buen camino si puedes decir:

```text
“PetMatch usa ownership sobre Pet y SupportRequest.
En mi aplicación el mismo patrón protege RepairOrder porque cada orden pertenece a un cliente/técnico específico.”
```

O:

```text
“PetMatch bloquea SupportRequest al aceptar una postulación.
Mi dominio no tiene una competencia equivalente, así que no necesito locking.”
```

La segunda respuesta puede demostrar más comprensión que copiar la misma solución.

---

# 67. Ruta de repaso cuando te bloquees

Si el problema está en:

```text
arranque / Beans / Maven
→ Fundamentos

Entities / relaciones / DB
→ Dominio y persistencia

formularios / templates
→ Web MVC

login / ownership
→ Seguridad

JSON / status / errores API
→ REST

tests / diagnóstico
→ Calidad

Git / términos / alternativas
→ Referencia
```

Usa el libro como mapa, no como código que debas reproducir línea por línea.

---

# 68. Entrega mínima para demostrar autonomía

Al finalizar deberías tener:

```text
repositorio Git propio
+
README con problema y ejecución
+
aplicación Spring Boot funcional
+
modelo persistente propio
+
flujo de negocio propio
+
interfaz web
+
seguridad si el dominio la requiere
+
API REST si el alcance la requiere
+
pruebas con fronteras explicables
+
historial de commits
```

Y, sobre todo, deberías poder abrir cualquier clase principal y responder:

> **¿por qué existe?**

---

# 69. Cierre

PetMatch te mostró una implementación completa.

Las prácticas de `BookShelf` demostraron que los mecanismos podían trasladarse.

Esta ruta completa el tercer paso:

```text
comprender una aplicación
↓
reproducir patrones en otro dominio guiado
↓
construir una aplicación propia
```

Ese último paso es el objetivo del libro.

---

## Vuelve al libro cuando necesites profundizar

- [Cómo usar este libro](00-como-usar-este-libro.md)
- [Fundamentos](01-fundamentos/README.md)
- [Dominio y persistencia](02-dominio-y-persistencia/README.md)
- [Web MVC](03-web-mvc/README.md)
- [Seguridad](04-seguridad/README.md)
- [REST](05-rest/README.md)
- [Calidad y recorrido completo](06-calidad-y-recorrido/README.md)
- [Referencia](07-referencia/README.md)

---

[← Portada e índice](README.md) · [Cómo usar este libro](00-como-usar-este-libro.md)
