# Práctica de transferencia — crea una aplicación Spring Boot desde cero

Este capítulo práctico acompaña los capítulos 01–08 sin reemplazarlos.

Su objetivo es comprobar una habilidad distinta a leer PetMatch:

> **¿Puedes construir una base Spring Boot pequeña en otro dominio y explicar qué hace cada paso?**

Usaremos `BookShelf` como ejemplo neutral. Puedes sustituirlo por tu propio proyecto siempre que mantengas el mismo objetivo técnico.

---

## Qué vas a demostrar

Al finalizar deberías poder:

```text
crear proyecto
→ comprobar Java
→ ejecutar Maven Wrapper
→ reconocer estructura
→ leer pom.xml
→ configurar propiedades
→ crear Controller
→ crear Service
→ usar constructor injection
→ comprobar el endpoint
→ diagnosticar un Bean faltante
```

No implementarás todavía JPA, autenticación ni una aplicación completa.

---

# 1. Antes de abrir Spring Initializr: define un dominio mínimo

No empieces eligiendo anotaciones.

Escribe primero una historia pequeña.

Ejemplo:

```text
BookShelf administra libros.
Un usuario podrá consultar información de libros.
Más adelante podrá existir préstamo, disponibilidad y persistencia.
```

Conceptos iniciales:

```text
Book
```

Posibles conceptos futuros:

```text
Member
Loan
```

Regla para esta práctica:

> No crees clases para conceptos que todavía no necesitas.

---

# 2. Genera el proyecto

Abre Spring Initializr en:

```text
start.spring.io
```

Usa como referencia:

```text
Project: Maven
Language: Java
Spring Boot: 4.1.1
Packaging: Jar
Java: 21
```

Metadatos de ejemplo:

```text
Group: com.example
Artifact: bookshelf
Name: BookShelf
Package name: com.example.bookshelf
```

Dependencia mínima para esta práctica:

```text
Spring Web MVC
```

Puedes seleccionar además otras dependencias si tu proyecto las necesita, pero no las agregues solamente porque aparecen en PetMatch.

> [!IMPORTANT]
> Initializr genera una base ejecutable. No genera tu dominio, tus reglas de negocio ni una arquitectura completa.

---

# 3. Descomprime y reconoce la estructura

Deberías encontrar una estructura semejante a:

```text
bookshelf/
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/bookshelf/
    │   │       └── BookShelfApplication.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/
```

Identifica:

```text
pom.xml
→ modelo Maven

mvnw / mvnw.cmd
→ Maven Wrapper

src/main/java
→ código de aplicación

src/main/resources
→ configuración/recursos

src/test/java
→ pruebas
```

No crees todavía `controller/`, `service/`, `repository/`, `dto/`, `security/` y otras carpetas de PetMatch de una sola vez.

---

# 4. Comprueba Java

Ejecuta:

```bash
java -version
```

Debes poder responder:

```text
¿Qué JDK está ejecutando esta terminal?
¿Es compatible con Java 21?
```

Si el JDK no coincide, resuelve el entorno antes de modificar código Spring.

---

# 5. Ejecuta el test generado

Linux/macOS/WSL:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

Lectura conceptual:

```text
mvnw
→ usa el Wrapper del proyecto

test
→ ejecuta el ciclo Maven necesario hasta la fase de tests
```

Resultado esperado:

```text
el proyecto compila y los tests generados terminan correctamente
```

Si falla, lee el error antes de agregar dependencias o anotaciones al azar.

---

# 6. Arranca la aplicación vacía

Linux/macOS/WSL:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Lectura:

```text
spring-boot:run
→ objetivo del Spring Boot Maven Plugin
→ inicia la aplicación configurada
```

Detén el proceso con:

```text
Ctrl + C
```

En este punto debes poder localizar la clase:

```java
@SpringBootApplication
public class BookShelfApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookShelfApplication.class, args);
    }
}
```

---

# 7. Lee el `pom.xml` antes de modificarlo

Localiza:

```text
parent
java.version
groupId
artifactId
version
dependencies
build/plugins
```

Responde:

```text
¿Qué versión de Spring Boot usa?
¿Qué versión Java declara?
¿Qué dependencia habilita Web MVC?
¿Qué plugin permite integrar tareas Spring Boot con Maven?
```

No agregues versiones manuales a cada starter si están gestionadas por Spring Boot.

---

# 8. Comprende los comandos básicos

## Solo tests

```bash
./mvnw test
```

## Eliminar build anterior

```bash
./mvnw clean
```

## Limpiar y volver a probar

```bash
./mvnw clean test
```

## Empaquetar

```bash
./mvnw package
```

## Limpiar y empaquetar

```bash
./mvnw clean package
```

## Arrancar la aplicación

```bash
./mvnw spring-boot:run
```

Después de `package` o `clean package`, observa:

```text
target/
```

Pregunta:

> ¿Por qué `target/` es contenido generado y no código fuente?

---

# 9. Cambia la configuración de forma controlada

Si Initializr generó:

```text
application.properties
```

puedes conservarlo o migrar a YAML.

Para practicar YAML, crea:

```text
src/main/resources/application.yaml
```

con:

```yaml
spring:
  application:
    name: bookshelf

server:
  port: 8081
```

Si conservas `application.properties`, evita duplicar las mismas propiedades en ambos formatos.

Arranca nuevamente:

```bash
./mvnw spring-boot:run
```

Comprueba que el servidor usa:

```text
8081
```

---

# 10. Crea una primera responsabilidad: Service

Crea el package:

```text
com.example.bookshelf.service
```

Puedes hacerlo desde el IDE. No necesitas memorizar comandos de shell para crear packages.

Crea:

```java
package com.example.bookshelf.service;

import org.springframework.stereotype.Service;

@Service
public class BookService {

    public String getMessage() {
        return "Books are ready";
    }
}
```

Preguntas:

```text
¿Por qué esta clase está en service?
¿Qué responsabilidad tiene?
¿Qué significa @Service para Spring?
```

---

# 11. Crea un Controller que dependa del Service

Crea el package:

```text
com.example.bookshelf.controller
```

Y la clase:

```java
package com.example.bookshelf.controller;

import com.example.bookshelf.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books-message")
    public String message() {
        return bookService.getMessage();
    }
}
```

Observa lo que NO aparece:

```java
new BookService()
```

El Controller declara una dependencia.

Spring la resuelve.

---

# 12. Comprueba la inyección

Arranca la aplicación.

Si usaste el puerto 8081, abre:

```text
http://localhost:8081/books-message
```

Resultado esperado:

```text
Books are ready
```

Acabas de comprobar:

```text
component scanning
+
@RestController
+
@Service
+
constructor injection
+
HTTP
```

---

# 13. Provoca un error para entender DI

Elimina temporalmente:

```java
@Service
```

de `BookService`.

Vuelve a arrancar.

Spring ya no debería disponer del mismo Bean detectado por component scanning para satisfacer:

```java
public BookController(BookService bookService)
```

Lee el error.

Luego restaura `@Service`.

La relación que debes entender es:

```text
BookController necesita BookService
↓
Spring busca Bean compatible
↓
BookService no está registrado
↓
dependencia no puede resolverse
↓
contexto falla
```

---

# 14. ¿Por qué no usamos Repository todavía?

Porque todavía no existe persistencia en esta práctica.

Crear:

```text
BookRepository
```

sin usar una base de datos no demostraría una responsabilidad real.

En el bloque 02 aprenderás cuándo introducir:

```text
@Entity
JpaRepository
relaciones
transacciones
```

Esta práctica termina antes de eso deliberadamente.

---

# 15. Relación con PetMatch

Compara:

```text
BookController
→ BookService
```

con:

```text
PetController
→ PetService
```

La idea transferida es:

```text
Controller recibe/coordina HTTP
Service concentra una responsabilidad de aplicación
Spring conecta ambos por constructor
```

No copiamos:

```text
Pet
SupportRequest
SupportApplication
```

porque pertenecen al dominio de PetMatch.

---

# 16. Variables de entorno: práctica adicional

Cuando más adelante agregues MySQL, evita hardcodear credenciales.

Un esquema como PetMatch sería:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

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

No necesitas agregar esta configuración hasta que la aplicación tenga persistencia.

---

# 17. Checklist de transferencia

Sin mirar PetMatch, intenta responder:

```text
[ ] sé crear un proyecto desde Initializr
[ ] sé identificar package raíz
[ ] sé comprobar java -version
[ ] sé ejecutar mvnw test
[ ] sé ejecutar spring-boot:run
[ ] sé explicar clean y package
[ ] sé localizar pom.xml
[ ] sé distinguir dependency de plugin
[ ] sé crear un package desde el IDE
[ ] sé crear un Service
[ ] sé crear un Controller
[ ] sé usar constructor injection
[ ] sé comprobar un endpoint
[ ] sé explicar por qué falla si el Bean no existe
[ ] sé distinguir estructura generada de estructura creada por mí
```

Si una casilla depende de copiar literalmente el código de PetMatch, vuelve al capítulo correspondiente y explica primero el concepto.

---

# 18. Qué NO debes hacer todavía

Evita convertir esta práctica en una aplicación completa antes de estudiar los siguientes bloques.

No agregues por impulso:

```text
JWT
OAuth2
microservices
Docker
Kafka
Redis
MapStruct
Testcontainers
```

Tampoco necesitas crear todas estas carpetas por adelantado:

```text
repository/
dto/
security/
exception/
config/
```

Agrega estructura cuando exista una responsabilidad real.

---

# 19. Evidencia de que comprendiste

La práctica está lograda si puedes explicar, sin leer código:

1. qué generó Initializr;
2. qué hace `mvnw`;
3. qué hace `spring-boot:run`;
4. qué diferencia existe entre Maven y Spring Boot;
5. qué papel tiene `@SpringBootApplication`;
6. qué papel tiene `@Service`;
7. por qué el Controller no hace `new BookService()`;
8. por qué el package raíz afecta component scanning;
9. qué parte construiste tú;
10. qué parte pertenece al framework.

---

# 20. Resultado esperado

Al terminar debes haber construido algo pequeño pero conceptualmente valioso:

```text
proyecto Spring Boot propio
↓
Maven Wrapper funcionando
↓
configuración entendible
↓
Controller
↓
constructor injection
↓
Service
↓
endpoint comprobable
```

Este resultado es deliberadamente simple.

La meta no es impresionar con muchas tecnologías. La meta es demostrar que puedes **recrear la base con comprensión**.

---

[← Capítulo 08 — Inyección de dependencias](08-inyeccion-de-dependencias.md) · [Índice del bloque](README.md) · [Siguiente bloque → Dominio y persistencia](../02-dominio-y-persistencia/README.md)
