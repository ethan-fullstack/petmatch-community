# Práctica de transferencia — usa la referencia sobre tu propio proyecto

Esta práctica acompaña los capítulos 36–39 y cierra el recorrido de `BookShelf`.

No añade una nueva capa de Spring ni una nueva tecnología. El objetivo es comprobar que ya puedes **orientarte, versionar, explicar y evaluar** una aplicación propia sin depender de los nombres de PetMatch.

Trabajarás con cuatro habilidades:

```text
Git/GitHub
→ conservar y revisar cambios

Glosario
→ explicar términos con tus propias clases

Índice de clases y conceptos
→ encontrar dónde vive una responsabilidad

Evoluciones
→ distinguir lo implementado de lo que solo sería una opción futura
```

---

# 1. Relación con los capítulos 36–39

| Capítulo | Pregunta | Práctica en `BookShelf` |
|---|---|---|
| 36 | ¿cómo versiono cambios de forma segura? | revisar `status`, `diff`, `add`, `commit`, ramas y secretos |
| 37 | ¿qué significa este término? | construir definiciones desde código propio |
| 38 | ¿dónde vive este concepto? | mapear concepto ↔ clase ↔ responsabilidad |
| 39 | ¿qué podría evolucionar? | separar necesidades reales de tecnologías posibles |

---

# 2. Empieza desde un repositorio limpio

En tu proyecto `BookShelf` ejecuta:

```bash
git status
```

Debes poder interpretar:

```text
On branch ...
Changes not staged for commit
Changes to be committed
Untracked files
nothing to commit
```

No leas `git status` como un mensaje decorativo. Úsalo para responder:

```text
¿qué cambié?
¿qué está preparado para commit?
¿qué todavía no conoce Git?
```

---

# 3. Revisa el diff antes de agregar

Ejecuta:

```bash
git diff
```

Después, si ya hiciste `git add`, revisa:

```bash
git diff --staged
```

La secuencia recomendada para un cambio pequeño es:

```text
editar
→ ejecutar pruebas pertinentes
→ git status
→ git diff
→ git add
→ git diff --staged
→ git commit
```

El objetivo no es memorizar comandos, sino evitar commits que mezclen cambios no relacionados.

---

# 4. Haz un commit coherente

Supón que agregaste los tests del bloque anterior.

Un commit puede ser:

```bash
git add src/test

git commit -m "test: cover book loan flow"
```

Evita mensajes como:

```text
changes
update
final
cosas
```

El mensaje debería permitir reconocer la intención principal sin abrir inmediatamente el diff.

---

# 5. Git no reemplaza las pruebas

Esta secuencia es incorrecta como criterio de calidad:

```text
git commit exitoso
→ aplicación correcta
```

Git registra cambios. No demuestra reglas de negocio.

Antes de un commit relacionado con código ejecuta la evidencia pertinente, por ejemplo:

```bash
./mvnw test
```

O una clase concreta:

```bash
./mvnw -Dtest=LoanServiceTests test
```

---

# 6. Revisa `.gitignore`

En `BookShelf`, identifica categorías que no deberían versionarse por defecto:

```text
target/
archivos del IDE
credenciales locales
archivos temporales
```

Si utilizas un `.env` local para variables, debería tratarse como información local/sensible.

Pero recuerda:

```text
agregar algo a .gitignore
≠
eliminarlo de commits anteriores
```

Si una credencial ya fue publicada, ignorar el archivo después no deshace la exposición.

---

# 7. Comprueba que no estás versionando secretos

Antes de `git add .`, revisa:

```bash
git status
```

Busca especialmente archivos que puedan contener:

```text
DB_PASSWORD
API keys
tokens
credenciales
```

Un proyecto didáctico también debe practicar hábitos seguros.

---

# 8. `main` no es “GitHub”

Debes poder explicar:

```text
Git
→ sistema de control de versiones

GitHub
→ plataforma que aloja repositorios Git y facilita colaboración

main
→ nombre convencional de una rama principal en muchos repositorios
```

No confundas herramienta, plataforma y rama.

---

# 9. Branch para un cambio aislado

Si practicas un flujo con ramas:

```bash
git switch -c test/loan-api
```

Realiza un cambio pequeño, revisa y crea un commit.

Después puedes volver:

```bash
git switch main
```

La habilidad importante es comprender que una branch es una línea de trabajo basada en commits, no una copia manual de carpetas.

---

# 10. `fetch`, `pull` y `push`

Explica la diferencia antes de usarlos:

```text
git fetch
→ obtiene referencias/cambios remotos sin integrarlos automáticamente en tu rama actual

git pull
→ obtiene e integra según la configuración/estrategia aplicable

git push
→ publica commits locales hacia un remoto autorizado
```

No uses `pull` como solución automática a cualquier problema de Git sin mirar primero el estado del repositorio.

---

# 11. Checkpoint 36 — Git y GitHub

Sin consultar el capítulo responde:

```text
¿Qué diferencia hay entre working tree y staging?
¿Qué muestra git status?
¿Qué diferencia hay entre git diff y git diff --staged?
¿Por qué target/ normalmente se ignora?
¿Por qué .gitignore no borra un secreto ya committeado?
¿Qué diferencia hay entre Git y GitHub?
```

Revisa [capítulo 36](36-git-github-y-versionado.md).

---

# 12. Construye un mini-glosario de `BookShelf`

No copies definiciones del glosario de PetMatch cambiando nombres.

Elige al menos diez términos realmente presentes en tu proyecto:

```text
Bean
Entity
Repository
Service
DTO
Authentication
ownership
@Transactional
EntityGraph
ProblemDetail
MockMvc
```

Para cada uno responde:

```text
qué significa
para qué sirve
dónde aparece en BookShelf
qué error ayuda a evitar comprenderlo
```

---

# 13. Ejemplo — Bean

Una definición transferida podría ser:

```text
Bean
→ objeto administrado por Spring
→ puede ser descubierto como componente o creado con @Bean
→ en BookShelf, BookService es un Bean detectado por component scanning
→ PasswordEncoder puede ser un Bean producido desde SecurityConfig
```

Una definición débil sería:

> “Bean es una clase de Spring.”

Eso no explica el concepto ni su relación con tus objetos.

---

# 14. Ejemplo — ownership

```text
ownership
→ autorización ligada a la relación entre usuario actual y recurso
→ no se resuelve solo con ROLE_USER
→ en BookShelf, LoanService obtiene el usuario autenticado
→ Repository consulta Loan por loanId + borrowerId
```

Aquí puedes señalar clases concretas:

```text
LoanService
LibraryUserService
LoanRepository
```

---

# 15. Ejemplo — DTO

No definas DTO como:

> “clase para datos”.

Explica sus contratos concretos:

```text
BookForm
→ entrada MVC

BookApiRequest
→ entrada JSON

BookApiResponse
→ salida JSON
```

Y compáralos con:

```text
Book
→ Entity persistente
```

---

# 16. Ejemplo — `@Transactional`

```text
@Transactional
→ delimita una operación transaccional administrada por Spring
→ permite que cambios relacionados formen una unidad de trabajo
→ en borrowBook, Book pasa a LOANED y Loan se crea ACTIVE dentro del mismo caso de uso
```

No reduzcas la definición a:

> “sirve para guardar en base de datos”.

---

# 17. Checkpoint 37 — glosario

Toma cinco términos al azar y explícalos sin usar nombres de PetMatch.

Si solo puedes definir:

```text
SupportRequest
PetService
PetRestController
```

pero no puedes encontrar el equivalente conceptual en `BookShelf`, todavía falta transferencia.

Revisa [capítulo 37](37-glosario.md).

---

# 18. Crea un mapa de clases de `BookShelf`

Haz una tabla como esta:

| Clase | Responsabilidad principal | Conceptos |
|---|---|---|
| `BookShelfApplication` | arranque | `@SpringBootApplication`, component scanning |
| `SecurityConfig` | políticas de seguridad | `SecurityFilterChain`, `PasswordEncoder`, `@Order` |
| `BookController` | interfaz MVC de libros | MVC, Model, Form DTO |
| `BookRestController` | interfaz REST de libros | JSON, API DTO, status HTTP |
| `BookService` | reglas de libros | Service, transacción, validación de negocio |
| `LoanService` | préstamos y ownership | estado, transacción, ownership |
| `BookRepository` | persistencia de Book | JPA, query, lock |
| `LoanRepository` | persistencia de Loan | JPA, ownership query, `EntityGraph` |
| `ApiExceptionHandler` | traducción de errores REST | `ProblemDetail` |

No es una lista de archivos. Es un mapa de responsabilidades.

---

# 19. Navega concepto → clase

Responde sin buscar por nombre de archivo:

```text
¿dónde estudiarías ownership?
→ LoanService + LoanRepository + LibraryUserService

¿dónde estudiarías HTTP Basic?
→ SecurityConfig

¿dónde estudiarías JSON de salida?
→ BookApiResponse + ApiDtoMapper + BookRestController

¿dónde estudiarías locking?
→ BookRepository + LoanService
```

Ese movimiento conceptual es el propósito del índice.

---

# 20. Navega clase → concepto

Haz la dirección contraria.

Si abres:

```text
BookRepository
```

deberías reconocer:

```text
JpaRepository
query derivation
JPQL
PESSIMISTIC_WRITE
```

Si abres:

```text
SecurityConfig
```

deberías reconocer:

```text
PasswordEncoder
dos SecurityFilterChain
form login
HTTP Basic
STATELESS
CSRF según interfaz
```

---

# 21. Checkpoint 38 — índice de clases y conceptos

Elige cinco conceptos:

```text
DI
LAZY
ownership
ProblemDetail
MockMvc
```

Para cada uno responde:

```text
¿qué clase abrirías primero?
¿por qué?
¿qué otra clase necesitas después?
```

Después haz lo contrario con cinco clases.

Revisa [capítulo 38](38-indice-de-clases-y-conceptos.md).

---

# 22. Separa tres categorías

Al evaluar `BookShelf`, clasifica cada idea en:

```text
A. implementado
B. necesidad real todavía no resuelta
C. tecnología posible sin necesidad demostrada
```

Esta clasificación evita convertir el proyecto en una colección de herramientas.

---

# 23. Ejemplo de clasificación

## Implementado

```text
MySQL
JPA/Hibernate
MVC/Thymeleaf
form login
HTTP Basic API
ownership
ProblemDetail
Mockito
MockMvc
```

## Necesidad posible

Supón que el equipo detecta:

```text
los cambios de schema deben reproducirse exactamente entre ambientes
```

Esa sí es una necesidad concreta.

## Tecnología candidata

A partir de esa necesidad podrías evaluar:

```text
Flyway
Liquibase
```

El orden correcto es:

```text
problema
→ necesidad
→ alternativas
```

no:

```text
herramienta popular
→ buscar dónde ponerla
```

---

# 24. Evalúa Testcontainers sin implementarlo

Pregunta:

```text
¿Qué problema resolvería?
```

Posible respuesta:

```text
las pruebas de integración dependen de una DB externa configurada manualmente
```

Beneficios posibles:

```text
entorno reproducible
motor temporal controlado
mejor aislamiento
```

Costes:

```text
runtime de contenedores
arranque adicional
más infraestructura de test
```

Eso es una evaluación técnica. No necesitas agregar Testcontainers para demostrar que entendiste la decisión.

---

# 25. Evalúa JWT sin asumir que REST lo necesita

`BookShelf` puede tener:

```text
HTTP Basic
+
STATELESS
```

para la API didáctica actual.

La existencia de JSON no implica automáticamente:

```text
JWT obligatorio
```

Primero necesitarías una razón, por ejemplo requisitos de interoperabilidad, arquitectura cliente separada o un modelo concreto de emisión/validación de tokens.

Después compararías alternativas.

---

# 26. Evalúa optimistic locking

Ahora utilizas:

```text
PESSIMISTIC_WRITE sobre Book
```

Una alternativa futura podría ser:

```java
@Version
```

Pero la pregunta no es cuál “es más moderno”.

Pregunta:

```text
¿qué patrón de competencia existe?
¿cuánto tiempo se retiene el lock?
¿qué frecuencia de conflicto se espera?
¿cómo manejará la aplicación un conflicto optimista?
```

---

# 27. Evalúa migraciones de DB

Si el proyecto sigue usando:

```text
ddl-auto: update
```

puedes explicar por qué una aplicación con despliegues controlados podría necesitar migraciones versionadas.

Pero distingue:

```text
explicar una alternativa
≠
tenerla implementada
```

---

# 28. Evalúa cobertura adicional

Después de la práctica de Calidad puedes detectar huecos reales como:

```text
no hay test concurrente real del lock
no todos los endpoints tienen test específico
no hay browser end-to-end
```

Eso es más útil que decir:

> “faltan más tests”.

Una mejora debe responder:

```text
qué riesgo quiero cubrir
qué test demostraría ese riesgo
qué infraestructura requiere
```

---

# 29. Checkpoint 39 — evoluciones

Elige tres tecnologías:

```text
Testcontainers
JWT
Flyway
```

Para cada una responde:

```text
¿Qué problema concreto resolvería en BookShelf?
¿Ese problema existe hoy o lo estoy suponiendo?
¿Qué alternativa más simple existe?
¿Qué coste introduce?
¿Cómo probaría la evolución?
```

Revisa [capítulo 39](39-posibles-evoluciones-no-implementadas.md).

---

# 30. Prueba de cierre — explica tu proyecto sin abrir PetMatch

Explica `BookShelf` en este orden:

```text
problema del dominio
→ Entities
→ relaciones
→ Repositories
→ Services y reglas
→ transacciones/estados
→ MVC
→ validación
→ autenticación
→ ownership
→ REST
→ errores HTTP
→ pruebas
→ Git
→ posibles evoluciones
```

Para cada parte señala al menos una clase propia.

Si necesitas abrir PetMatch para recordar cada nombre, úsalo como referencia; después vuelve a hacer la explicación sin copiar su dominio.

---

# 31. Evidencia final de autonomía

Deberías poder recibir una pregunta como:

> “¿Dónde se controla que un usuario no devuelva el préstamo de otra persona?”

Y responder recorriendo:

```text
Authentication
→ LibraryUserService
→ LoanService
→ LoanRepository
→ ownership query
```

También deberías poder responder:

> “¿Qué prueba demuestra eso?”

sin inventar cobertura. Si todavía no existe un test específico de ownership para esa operación, dilo y describe qué test necesitarías.

---

# 32. Checklist de salida

Comprueba que puedes:

- [ ] interpretar `git status`;
- [ ] revisar `git diff` y `git diff --staged`;
- [ ] crear un commit coherente;
- [ ] distinguir Git de GitHub;
- [ ] explicar branch, fetch, pull y push;
- [ ] identificar archivos que no deberían versionarse;
- [ ] explicar por qué `.gitignore` no borra historial;
- [ ] definir términos usando clases de `BookShelf`;
- [ ] navegar concepto → clase;
- [ ] navegar clase → concepto;
- [ ] construir un mapa de responsabilidades;
- [ ] distinguir implementado, necesidad y alternativa;
- [ ] evaluar una tecnología por problema/beneficio/coste;
- [ ] reconocer huecos de cobertura sin exagerarlos;
- [ ] explicar `BookShelf` de extremo a extremo sin copiar PetMatch;
- [ ] decir con precisión qué evidencia respalda una afirmación.

---

## Resultado esperado

Al terminar los capítulos 36–39 y esta práctica ya no deberías depender del libro para responder solamente:

```text
“qué anotación se usa”
```

Deberías poder responder preguntas más útiles:

```text
¿dónde vive esta responsabilidad?
¿qué capa debería cambiar?
¿qué test demuestra esta regla?
¿qué parte está implementada?
¿qué parte solo es una posibilidad?
¿qué trade-off aceptamos?
¿cómo versiono el cambio sin mezclarlo con otros?
```

Ese es el objetivo del bloque de Referencia: convertir el libro en una herramienta de consulta y no en una receta que debas copiar de principio a fin cada vez.

---

## Siguiente paso — deja el dominio guiado

`BookShelf` fue una práctica de transferencia guiada. El cierre del libro consiste en repetir el recorrido sin recibir un dominio, clases ni reglas ya elegidas.

Continúa con:

**[Construye tu propia aplicación con Spring Boot →](../construye-tu-propia-aplicacion.md)**

---

[← Índice de Referencia](README.md) · [Índice general](../README.md) · [Capítulo 36](36-git-github-y-versionado.md) · [Ruta autónoma →](../construye-tu-propia-aplicacion.md)
