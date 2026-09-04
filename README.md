# PetMatch Community

PetMatch Community es una aplicación demostrativa construida con Spring Boot para gestionar solicitudes comunitarias de apoyo temporal para mascotas.

La aplicación expone dos interfaces sobre la misma lógica de negocio:

- una interfaz web MVC con Thymeleaf;
- una API REST JSON bajo `/api/v1`.

## Tecnologías

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Thymeleaf
- Spring Data JPA
- Spring Security
- Bean Validation
- MySQL
- Maven Wrapper
- Tailwind CSS mediante CDN

## Requisitos

Para ejecutar el proyecto necesitas:

- JDK 21
- MySQL en ejecución
- Git

No necesitas instalar Maven globalmente porque el proyecto incluye Maven Wrapper.

Tampoco necesitas Node.js, npm, Vite o PostCSS. Tailwind CSS se carga desde CDN porque esta aplicación tiene propósito demostrativo y académico.

## Base de datos

Crea una base de datos MySQL:

```sql
CREATE DATABASE petmatch_community
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

La aplicación utiliza las siguientes variables de entorno:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Ejemplo de URL local:

```text
jdbc:mysql://localhost:3306/petmatch_community
```

## Windows 11 — PowerShell

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/petmatch_community"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="TU_PASSWORD_LOCAL"

.\mvnw.cmd spring-boot:run
```

Para ejecutar pruebas:

```powershell
.\mvnw.cmd clean test
```

## Windows 11 — CMD

```bat
set DB_URL=jdbc:mysql://localhost:3306/petmatch_community
set DB_USERNAME=root
set DB_PASSWORD=TU_PASSWORD_LOCAL
mvnw.cmd spring-boot:run
```

## Linux, macOS o WSL

```bash
export DB_URL="jdbc:mysql://localhost:3306/petmatch_community"
export DB_USERNAME="root"
export DB_PASSWORD="TU_PASSWORD_LOCAL"

./mvnw spring-boot:run
```

Si utilizas mise, el repositorio incluye `mise.toml` para seleccionar Temurin Java 21. Mise es opcional y no forma parte de los requisitos del proyecto.

## Interfaz web

Con la aplicación en ejecución abre:

```text
http://localhost:8080
```

La interfaz web usa:

```text
form login
+ sesión HTTP
+ CSRF habilitado
+ Thymeleaf
```

## API REST

La API está versionada bajo:

```text
http://localhost:8080/api/v1
```

La API utiliza HTTP Basic y sesiones stateless. Para esta aplicación académica se utiliza HTTP en entorno local. En un sistema real, HTTP Basic debe utilizarse únicamente sobre HTTPS.

Los usuarios de la API son los mismos usuarios registrados desde la aplicación web. El correo funciona como nombre de usuario.

Ejemplo con `curl`:

```bash
curl -u user@example.com:testing123 \
  http://localhost:8080/api/v1/pets
```

### Mascotas

```text
GET    /api/v1/pets
POST   /api/v1/pets
GET    /api/v1/pets/{petId}
PUT    /api/v1/pets/{petId}
DELETE /api/v1/pets/{petId}
```

Ejemplo para crear una mascota:

```bash
curl -u user@example.com:testing123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Luna",
    "species": "Perro",
    "age": 4,
    "description": "Sociable y acostumbrada a caminar por las tardes."
  }' \
  http://localhost:8080/api/v1/pets
```

### Solicitudes de apoyo

```text
GET  /api/v1/support-requests
GET  /api/v1/support-requests/mine
GET  /api/v1/support-requests/{requestId}
POST /api/v1/support-requests
PUT  /api/v1/support-requests/{requestId}
POST /api/v1/support-requests/{requestId}/cancel
POST /api/v1/support-requests/{requestId}/complete
```

Ejemplo para crear una solicitud:

```bash
curl -u user@example.com:testing123 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Paseo para Luna",
    "description": "Necesito apoyo durante la tarde.",
    "supportType": "WALK",
    "serviceDate": "2026-09-10T15:30:00",
    "petId": 1
  }' \
  http://localhost:8080/api/v1/support-requests
```

### Postulaciones

```text
GET  /api/v1/support-applications/mine
POST /api/v1/support-requests/{requestId}/applications
GET  /api/v1/support-requests/{requestId}/applications
POST /api/v1/support-applications/{applicationId}/accept
POST /api/v1/support-applications/{applicationId}/reject
```

Ejemplo para postularse:

```bash
curl -u helper@example.com:testing123 \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tengo disponibilidad y puedo ayudar con Luna."
  }' \
  http://localhost:8080/api/v1/support-requests/1/applications
```

### Códigos HTTP principales

```text
200 OK            consulta o actualización correcta
201 Created       recurso creado
204 No Content    acción completada sin cuerpo de respuesta
400 Bad Request   JSON o validación inválida
401 Unauthorized  autenticación requerida o incorrecta
404 Not Found     recurso inexistente o no visible para el usuario
409 Conflict      regla de negocio o transición de estado inválida
```

Los errores REST utilizan `ProblemDetail`. Un error de validación puede verse así:

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "errors": {
    "name": "El nombre es obligatorio"
  }
}
```

## Flujo principal del MVP

1. Un usuario se registra e inicia sesión.
2. Registra una mascota propia.
3. Publica una solicitud de apoyo asociada a esa mascota.
4. Otros usuarios pueden ver solicitudes abiertas y postularse.
5. El propietario revisa las postulaciones recibidas.
6. Al aceptar una postulación:
   - la postulación seleccionada pasa a `ACCEPTED`;
   - las demás pendientes pasan a `REJECTED`;
   - la solicitud pasa a `IN_PROGRESS`.
7. El propietario marca finalmente la solicitud como `COMPLETED`.

El mismo flujo puede ejecutarse desde la interfaz web o mediante la API REST porque ambas reutilizan la capa `Service`.

## Reglas principales

- Un usuario solo puede modificar o eliminar sus propias mascotas.
- Una mascota con solicitudes asociadas no puede eliminarse.
- Una solicitud solo puede usar una mascota propiedad del usuario autenticado.
- No se puede postular a una solicitud propia.
- Un usuario no puede postularse dos veces a la misma solicitud.
- Las solicitudes vencidas no aceptan postulaciones nuevas.
- Solo el propietario de la solicitud puede aceptar o rechazar postulaciones.
- Solo una postulación puede ser aceptada por solicitud.
- Las solicitudes canceladas rechazan sus postulaciones pendientes.
- La autorización se valida en backend; no depende únicamente de la interfaz web.

## Arquitectura

```text
Browser
   |
   v
MVC Controllers ----> Thymeleaf ----> HTML
   |
   |        misma lógica de negocio
   v
Services ----------------------------> Repositories ----> MySQL
   ^
   |
REST Controllers ----> API DTOs ----> JSON
   ^
   |
Postman / curl / otro cliente HTTP
```

Los controllers REST no serializan entidades JPA directamente. La API utiliza DTOs propios para mantener separado el modelo de persistencia del contrato JSON.

## Estructura principal

```text
src/main/java/com/petmatch/community/
├── config
├── controller
│   └── api
├── dto
│   └── api
├── exception
├── model
├── repository
├── security
└── service

src/main/resources/
├── templates
│   └── fragments
└── application.yaml
```

## Seguridad web y API

La configuración tiene dos cadenas de seguridad:

```text
/api/**
→ HTTP Basic
→ STATELESS
→ CSRF deshabilitado

resto de la aplicación
→ form login
→ sesión HTTP
→ CSRF habilitado
```

La separación permite demostrar dos mecanismos de acceso sin duplicar usuarios ni reglas de negocio.

## Configuración de desarrollo

Actualmente Hibernate utiliza:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
```

`ddl-auto: update` se utiliza por practicidad durante esta etapa demostrativa. En un proyecto destinado a producción convendría sustituirlo por migraciones controladas, por ejemplo con Flyway o Liquibase.

## Tailwind CSS

Las vistas usan Tailwind CSS mediante Play CDN, centralizado en un fragment Thymeleaf:

```html
<script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
```

Esto facilita experimentar con clases de utilidad sin instalar herramientas frontend. Requiere conexión a Internet para cargar los estilos y no es el enfoque recomendado para una aplicación de producción.

## Pruebas

El proyecto contiene:

- prueba de carga del contexto Spring;
- pruebas unitarias de reglas de solicitudes;
- pruebas unitarias de reglas de postulaciones;
- prueba de integración del flujo completo del MVP;
- pruebas de integración HTTP para la API REST y su coexistencia con MVC.

Ejecuta todas las pruebas con:

```bash
./mvnw clean test
```

En Windows:

```powershell
.\mvnw.cmd clean test
```

## Alcance

PetMatch Community no pretende ser una plataforma de adopción, tienda de mascotas, sistema de pagos, chat o red social. El objetivo es demostrar una aplicación Spring Boot con MVC, Thymeleaf, persistencia, autenticación, autorización, validaciones, reglas de negocio y una API REST sobre el mismo dominio.
