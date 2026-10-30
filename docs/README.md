# PetMatch Community — Spring Boot desde cero

> Un libro técnico y pedagógico para aprender Spring Boot recorriendo una aplicación real.

Este libro enseña **Spring Boot a través de una aplicación real llamada PetMatch Community** y añade prácticas para comprobar que los conceptos pueden trasladarse a otra aplicación.

No está planteado como un catálogo de anotaciones ni como documentación genérica del framework. Cada concepto se estudia a partir de una necesidad concreta del proyecto:

> **problema → concepto → mecanismo → código real → decisión → alternativa → error frecuente → transferencia**

PetMatch Community es una aplicación demostrativa para gestionar solicitudes comunitarias de apoyo temporal relacionadas con mascotas. La misma lógica de negocio se utiliza desde una interfaz web MVC con Thymeleaf y desde una API REST JSON.

> [!IMPORTANT]
> Si ya terminaste los bloques o quieres comprobar autonomía, utiliza la ruta **[Construye tu propia aplicación con Spring Boot](construye-tu-propia-aplicacion.md)**. Allí el dominio lo eliges tú y PetMatch deja de ser una plantilla para convertirse únicamente en referencia conceptual.

---

## ¿Para quién es este libro?

Está dirigido principalmente a aprendices que:

- conocen Java básico;
- conocen SQL básico;
- pueden leer clases, métodos, atributos y estructuras de control;
- todavía no conocen Spring Framework;
- todavía no conocen Spring Boot;
- todavía no conocen JPA/Hibernate;
- todavía no conocen Spring Security;
- todavía no conocen Thymeleaf;
- tienen poca o ninguna experiencia construyendo una aplicación web Java completa.

No necesitas saber previamente qué es un `Bean`, un contenedor de Spring, un `Repository`, un DTO, una sesión HTTP o un `SecurityFilterChain`. El libro introduce esos conceptos cuando aparece el problema que los hace necesarios.

> [!IMPORTANT]
> El objetivo no es memorizar anotaciones ni memorizar PetMatch. El objetivo es comprender **por qué existe cada pieza**, qué responsabilidad tiene, cómo se relaciona con las demás y cómo reconocer esa misma idea en otro proyecto.

---

## Conocimientos previos recomendados

Antes de empezar conviene sentirte cómodo con:

- clases y objetos en Java;
- constructores;
- interfaces;
- colecciones básicas;
- excepciones;
- `enum`;
- métodos y parámetros;
- conceptos SQL como tabla, fila, clave primaria y clave foránea;
- operaciones `SELECT`, `INSERT`, `UPDATE` y `DELETE` a nivel básico.

No necesitas dominar Maven, HTTP, MVC, ORM ni seguridad web. Esos temas se desarrollan progresivamente.

---

## La aplicación que estudiaremos

PetMatch Community permite representar un flujo como este:

1. un usuario se registra e inicia sesión;
2. registra una mascota propia;
3. publica una solicitud de apoyo para esa mascota;
4. otros usuarios consultan solicitudes abiertas;
5. uno o más usuarios se postulan;
6. el propietario acepta una postulación;
7. la aceptada pasa a `ACCEPTED`;
8. las demás pendientes pasan a `REJECTED`;
9. la solicitud pasa a `IN_PROGRESS`;
10. finalmente el propietario la marca como `COMPLETED`.

```mermaid
flowchart LR
    A[Usuario propietario] --> B[Registra Pet]
    B --> C[Crea SupportRequest OPEN]
    C --> D[Otros usuarios se postulan]
    D --> E[SupportApplication PENDING]
    E --> F[Propietario acepta una]
    F --> G[ACCEPTED]
    F --> H[Otras REJECTED]
    F --> I[SupportRequest IN_PROGRESS]
    I --> J[Propietario completa]
    J --> K[COMPLETED]
```

---

## Tecnologías del proyecto

| Tecnología | Uso en PetMatch |
|---|---|
| Java 21 | Lenguaje principal |
| Spring Boot 4.1.1 | Arranque, configuración y composición |
| Spring MVC | Interfaz web y manejo HTTP |
| Thymeleaf | Renderizado HTML server-side |
| Spring Data JPA | Acceso a persistencia |
| Hibernate | Implementación ORM usada por JPA |
| MySQL | Base de datos |
| Spring Security | Autenticación y autorización |
| Bean Validation | Validación de formularios y DTO |
| Maven | Gestión del proyecto y dependencias |
| Maven Wrapper | Ejecución reproducible de Maven |
| HTTP Session | Estado de autenticación web |
| HTTP Basic | Autenticación de API REST |
| ProblemDetail | Representación de errores REST |
| JUnit 5 | Pruebas |
| Mockito | Pruebas unitarias aisladas |
| MockMvc | Pruebas HTTP |
| Tailwind CSS vía CDN | Presentación visual |

---

## ¿Qué aprenderás?

Al terminar el recorrido deberías poder explicar, usando código real de PetMatch:

- qué problema intenta resolver Spring;
- qué aporta Spring Boot sobre Spring Framework;
- IoC, Dependency Injection, Beans y contenedor;
- estructura de un proyecto Spring Boot;
- Maven y `pom.xml`;
- configuración con `application.yaml`;
- arquitectura Controller → Service → Repository;
- modelo de dominio;
- JPA, Hibernate y ORM;
- relaciones y foreign keys;
- Spring Data JPA;
- reglas de negocio;
- transacciones y consistencia;
- máquinas de estado;
- concurrencia y locking;
- lazy loading y `EntityGraph`;
- MVC y Thymeleaf;
- Form DTO vs Entity vs API DTO;
- data binding y validación;
- autenticación y autorización;
- password hashing;
- ownership;
- CSRF y sesión;
- REST y JSON;
- seguridad REST;
- `ProblemDetail`;
- pruebas unitarias e integración;
- recorrido completo de una funcionalidad;
- decisiones arquitectónicas y sus trade-offs;
- errores frecuentes y diagnóstico por capas;
- Git, GitHub y versionado del proyecto;
- uso de un glosario y un índice de clases/conceptos;
- separación entre comportamiento implementado y posibles evoluciones;
- creación de una base Spring Boot propia sin copiar el dominio PetMatch;
- uso de Spring Initializr, Maven Wrapper, configuración y constructor injection en una aplicación distinta;
- modelado y persistencia de un dominio distinto mediante JPA y MySQL;
- creación de Entities, relaciones y Repositories fuera de PetMatch;
- aplicación de reglas, transacciones, estados, locking y `EntityGraph` en otro dominio;
- construcción de una interfaz MVC con Controller, Form DTO, Bean Validation, Model y Thymeleaf en otra aplicación;
- implementación de registro, `PasswordEncoder`, form login, roles y ownership fuera del dominio PetMatch;
- construcción de una API REST con Request/Response DTO, JSON, HTTP Basic stateless y `ProblemDetail` sobre Services ya existentes;
- construcción de pruebas unitarias e integración en otro dominio, explicando con precisión la frontera y evidencia de cada test;
- uso de Git, glosario, mapa concepto ↔ clase y evaluación de evoluciones sobre una aplicación propia;
- construcción autónoma de una aplicación propia desde el problema hasta una suite de pruebas defendible.

---

## Tecnologías y capacidades que no forman parte de PetMatch

No forman parte de la aplicación estudiada:

- carga de imágenes o archivos;
- `MultipartFile`;
- almacenamiento de imágenes;
- JWT;
- OAuth2;
- login social;
- microservicios;
- WebFlux;
- Flyway o Liquibase;
- Docker como requisito;
- Testcontainers;
- S3, MinIO o Cloudinary;
- CI/CD;
- observabilidad y métricas;
- 2FA;
- frontend React, Vue o Angular.

Estas tecnologías pueden estudiarse después como alternativas o extensiones, pero no son necesarias para comprender el recorrido principal.

---

# Cómo está organizado el libro

## 1. Fundamentos

- [01 — PetMatch y el problema](01-fundamentos/01-petmatch-y-el-problema.md)
- [02 — Antes de Spring](01-fundamentos/02-antes-de-spring.md)
- [03 — Spring y Spring Boot](01-fundamentos/03-spring-y-spring-boot.md)
- [04 — Estructura del proyecto](01-fundamentos/04-estructura-del-proyecto.md)
- [05 — Maven y dependencias](01-fundamentos/05-maven-y-dependencias.md)
- [06 — Configuración y `application.yaml`](01-fundamentos/06-configuracion-y-application-yaml.md)
- [07 — Arquitectura por capas](01-fundamentos/07-arquitectura-por-capas.md)
- [08 — Inyección de dependencias en PetMatch](01-fundamentos/08-inyeccion-de-dependencias.md)
- [Práctica de transferencia — crea una aplicación Spring Boot desde cero](01-fundamentos/practica-transferencia-fundamentos.md)

[Introducción del bloque 01](01-fundamentos/README.md)

---

## 2. Dominio y persistencia

- [09 — Modelo de dominio](02-dominio-y-persistencia/09-modelo-de-dominio.md)
- [10 — JPA y Hibernate](02-dominio-y-persistencia/10-jpa-y-hibernate.md)
- [11 — Relaciones JPA](02-dominio-y-persistencia/11-relaciones-jpa.md)
- [12 — Spring Data JPA](02-dominio-y-persistencia/12-spring-data-jpa.md)
- [13 — Service y reglas de negocio](02-dominio-y-persistencia/13-service-y-reglas-de-negocio.md)
- [14 — Transacciones y consistencia](02-dominio-y-persistencia/14-transacciones-y-consistencia.md)
- [15 — Máquinas de estado](02-dominio-y-persistencia/15-maquinas-de-estado.md)
- [16 — Concurrencia y locking](02-dominio-y-persistencia/16-concurrencia-y-locking.md)
- [17 — Lazy loading y `EntityGraph`](02-dominio-y-persistencia/17-lazy-loading-y-entitygraph.md)
- [Práctica de transferencia — dominio y persistencia con JPA](02-dominio-y-persistencia/practica-transferencia-dominio-persistencia.md)

[Introducción del bloque 02](02-dominio-y-persistencia/README.md)

---

## 3. Web MVC

- [18 — Spring MVC](03-web-mvc/18-spring-mvc.md)
- [19 — Thymeleaf](03-web-mvc/19-thymeleaf.md)
- [20 — Formularios y Form DTO](03-web-mvc/20-formularios-y-form-dto.md)
- [21 — Validación](03-web-mvc/21-validacion.md)
- [Práctica de transferencia — Web MVC con Thymeleaf](03-web-mvc/practica-transferencia-web-mvc.md)

[Introducción del bloque 03](03-web-mvc/README.md)

---

## 4. Seguridad

- [22 — Autenticación](04-seguridad/22-autenticacion.md)
- [23 — Spring Security](04-seguridad/23-spring-security.md)
- [24 — Contraseñas y `PasswordEncoder`](04-seguridad/24-contrasenas-y-password-encoder.md)
- [25 — Autorización y ownership](04-seguridad/25-autorizacion-y-ownership.md)
- [Práctica de transferencia — autenticación y autorización en BookShelf](04-seguridad/practica-transferencia-seguridad.md)
- [26 — CSRF, sesión y seguridad web](04-seguridad/26-csrf-sesion-y-seguridad-web.md)

[Introducción del bloque 04](04-seguridad/README.md)

---

## 5. REST

- [27 — REST API](05-rest/27-rest-api.md)
- [28 — DTO REST, JSON y mapping](05-rest/28-dto-rest-json-y-mapping.md)
- [29 — Seguridad REST](05-rest/29-seguridad-rest.md)
- [30 — `ProblemDetail` y errores HTTP](05-rest/30-problemdetail-y-errores-http.md)
- [Práctica de transferencia — REST, JSON y `ProblemDetail`](05-rest/practica-transferencia-rest.md)

[Introducción del bloque 05](05-rest/README.md)

---

## 6. Calidad y recorrido completo

- [31 — Pruebas unitarias](06-calidad-y-recorrido/31-pruebas-unitarias.md)
- [32 — Pruebas de integración](06-calidad-y-recorrido/32-pruebas-de-integracion.md)
- [33 — Flujo completo PetMatch](06-calidad-y-recorrido/33-flujo-completo-petmatch.md)
- [34 — Buenas prácticas y decisiones](06-calidad-y-recorrido/34-buenas-practicas-y-decisiones.md)
- [35 — Errores frecuentes](06-calidad-y-recorrido/35-errores-frecuentes.md)
- [Práctica de transferencia — calidad, pruebas y recorrido completo](06-calidad-y-recorrido/practica-transferencia-calidad-recorrido.md)

[Introducción del bloque 06](06-calidad-y-recorrido/README.md)

---

## 7. Referencia

- [36 — Git, GitHub y versionado](07-referencia/36-git-github-y-versionado.md)
- [37 — Glosario](07-referencia/37-glosario.md)
- [38 — Índice de clases y conceptos](07-referencia/38-indice-de-clases-y-conceptos.md)
- [39 — Posibles evoluciones no implementadas](07-referencia/39-posibles-evoluciones-no-implementadas.md)
- [Práctica de transferencia — usa la referencia sobre tu propio proyecto](07-referencia/practica-transferencia-referencia.md)

[Introducción del bloque 07](07-referencia/README.md)

## 8. Administración, reportes y calidad del software

- [40 — Administración de roles, autorización y auditoría](08-administracion-reportes-y-calidad/40-roles-autorizacion-y-auditoria.md)
- [41 — Reportes dinámicos con JPA: filtros, proyecciones y agrupaciones](08-administracion-reportes-y-calidad/41-reportes-dinamicos-jpa.md)
- [42 — Exportación de reportes PDF y Excel: Strategy cuando la variación es real](08-administracion-reportes-y-calidad/42-exportacion-reportes-pdf-excel-strategy.md)
- [43 — Usabilidad, navegación y accesibilidad](08-administracion-reportes-y-calidad/43-usabilidad-navegacion-y-accesibilidad.md)
- [44 — Modelos de calidad del software](08-administracion-reportes-y-calidad/44-modelos-de-calidad-del-software.md)
- [45 — Métricas e indicadores de calidad](08-administracion-reportes-y-calidad/45-metricas-e-indicadores-de-calidad.md)
- [46 — Verificación y validación de requisitos](08-administracion-reportes-y-calidad/46-verificacion-y-validacion-de-requisitos.md)
- [47 — Mejora continua con PDCA](08-administracion-reportes-y-calidad/47-mejora-continua-pdca.md)

[Introducción del bloque 08](08-administracion-reportes-y-calidad/README.md)

---

# Ruta de construcción autónoma

Después de estudiar los conceptos y completar las prácticas guiadas, utiliza:

**[Construye tu propia aplicación con Spring Boot →](construye-tu-propia-aplicacion.md)**

Esta ruta ya no te entrega un dominio. Debes definir tu propio problema, casos de uso, Entities, reglas, ownership y decisiones técnicas, y comprobarlos mediante ejecución y tests.

---

## Ruta recomendada

Lee el libro en orden la primera vez.

```mermaid
flowchart TD
    A[Fundamentos conceptuales] --> T1[Práctica: base Spring Boot]
    T1 --> B[Dominio y persistencia]
    B --> T2[Práctica: JPA y persistencia]
    T2 --> C[Web MVC]
    C --> T3[Práctica: MVC y Thymeleaf]
    T3 --> D[Seguridad 22–25]
    D --> T4[Práctica: autenticación y ownership]
    T4 --> S26[CSRF, sesión y seguridad web]
    S26 --> E[REST]
    E --> T5[Práctica: REST y ProblemDetail]
    T5 --> F[Pruebas y recorrido completo]
    F --> T6[Práctica: calidad y testing]
    T6 --> G[Referencia]
    G --> T7[Práctica: Git, mapa y evoluciones]
    T7 --> A1[Construye tu propia aplicación]
```

Después puedes usar el glosario y el índice de clases de forma no lineal.

> [!TIP]
> Mantén abierto el repositorio mientras lees. En las prácticas de transferencia abre además un proyecto Spring Boot propio y reproduce solamente los mecanismos estudiados, no el dominio de PetMatch.

---

## Convenciones del libro

Cuando aparezca una ruta como:

```text
src/main/java/com/petmatch/community/service/SupportRequestService.java
```

significa que debes localizar ese archivo real en el repositorio.

Los fragmentos marcados como **Código real** provienen del proyecto. Si se utiliza una simplificación se marca explícitamente como **pseudocódigo** o **ejemplo conceptual**.

Encontrarás bloques como:

> [!NOTE]
> Contexto adicional útil.

> [!TIP]
> Consejo práctico.

> [!WARNING]
> Error frecuente o riesgo.

> [!IMPORTANT]
> Idea clave para capítulos posteriores.

Y actividades:

- **🧠 Idea mental**;
- **🔎 En PetMatch**;
- **🛠 Prueba en el código**;
- **🔁 Llévalo a tu proyecto**;
- **🧪 Comprueba que entendiste**;
- **✅ Qué debes recordar**.

---

## Empieza aquí

**[Cómo usar este libro →](00-como-usar-este-libro.md)**

Luego:

**[Capítulo 01 — PetMatch y el problema →](01-fundamentos/01-petmatch-y-el-problema.md)**

Cuando termines las prácticas guiadas:

**[Construye tu propia aplicación con Spring Boot →](construye-tu-propia-aplicacion.md)**

---

[README principal del proyecto](../README.md) · [Cómo usar este libro →](00-como-usar-este-libro.md) · [Ruta autónoma →](construye-tu-propia-aplicacion.md)
