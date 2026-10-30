# Cómo usar este libro

Este libro está pensado para aprender Spring Boot de una forma práctica: **comprender un problema, localizar cómo se resolvió en PetMatch Community y después reconocer cómo aplicar la misma idea en otro proyecto**.

PetMatch es el hilo conductor, no una plantilla que debas copiar literalmente.

Si es tu primer contacto con Spring, sigue los capítulos en orden. Los conceptos aparecen cuando ya existe un problema concreto que justifica aprenderlos.

---

## La meta no es memorizar PetMatch

Al terminar un tema no basta con poder decir:

> “sé dónde está `PetService`”.

La meta es poder explicar algo más general:

```text
qué problema existe
→ qué responsabilidad aparece
→ qué mecanismo de Spring ayuda
→ cómo se ve en PetMatch
→ cómo lo reconocería en mi propio proyecto
```

Por ejemplo, después de estudiar arquitectura por capas deberías poder mirar una aplicación diferente —una biblioteca, un sistema de citas o un inventario— e identificar qué piezas podrían corresponder a Controller, Service y Repository.

> [!IMPORTANT]
> Aprender un framework no consiste en memorizar nombres de anotaciones. Consiste en comprender **qué responsabilidad representa cada pieza y por qué existe**.

---

## Trabaja con tres ventanas

Cuando sea posible, estudia con estas tres referencias abiertas:

1. **este libro**;
2. **el código de PetMatch Community**;
3. **un proyecto propio o de práctica** donde puedas reproducir ideas pequeñas.

Al principio la tercera ventana puede ser un proyecto vacío. Eso está bien.

El recorrido será progresivo:

```text
leer
↓
localizar en PetMatch
↓
explicar con tus palabras
↓
reproducir una versión pequeña
↓
comparar
```

No necesitas implementar PetMatch desde cero mientras lees. Las actividades te indicarán qué partes vale la pena reproducir.

---

## Dos formas de usar el libro

### Recorrido A — Comprender PetMatch

Úsalo si quieres estudiar una aplicación Spring Boot ya construida.

En cada capítulo:

1. lee el problema;
2. abre los archivos indicados;
3. sigue el código real;
4. responde las preguntas de comprobación.

### Recorrido B — Llevarlo a tu proyecto

Úsalo si estás desarrollando tu propia aplicación.

Después de comprender el ejemplo de PetMatch:

1. identifica el equivalente en tu dominio;
2. evita copiar nombres que no pertenecen a tu problema;
3. implementa solo el mecanismo que ya entiendes;
4. comprueba que puedes explicar por qué lo agregaste.

Ejemplo:

```text
PetMatch
PetController → PetService → PetRepository

Tu proyecto de biblioteca
BookController → BookService → BookRepository
```

El patrón puede transferirse. Los nombres y reglas de negocio no.

---

## El método de cada capítulo

Cuando el tema lo requiera, encontrarás una secuencia parecida a esta:

1. **¿Qué problema estamos resolviendo?**
2. **Idea mental.**
3. **Definición técnica.**
4. **¿Cómo lo resuelve Spring?**
5. **¿Dónde aparece en PetMatch?**
6. **Código real.**
7. **Paso a paso.**
8. **¿Por qué está diseñado así?**
9. **Alternativas y límites.**
10. **Errores frecuentes.**
11. **Prueba en el código.**
12. **Llévalo a tu proyecto.**
13. **Comprueba que entendiste.**
14. **Qué debes recordar.**

No todos los temas necesitan todas las secciones. Una idea sencilla puede resolverse en pocas páginas; temas como JPA, transacciones, seguridad o concurrencia necesitan más recorrido.

---

## Cómo identificar el tipo de código

### Código real de PetMatch

Cuando aparezca **Código real** o una ruta concreta del repositorio, el fragmento corresponde a la aplicación estudiada.

Ejemplo:

```java
public PetController(PetService petService) {
    this.petService = petService;
}
```

Ruta:

```text
src/main/java/com/petmatch/community/controller/PetController.java
```

### Ejemplo conceptual

Algunas veces reduciremos una idea para verla con claridad:

```text
petición HTTP
    ↓
Controller
    ↓
Service
    ↓
Repository
```

Esto explica una relación; no significa que exista un archivo con ese contenido.

### Ejemplo para tu proyecto

Cuando aparezca una sección **Llévalo a tu proyecto**, el código será deliberadamente genérico o utilizará otro dominio.

Por ejemplo:

```java
public BookService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
}
```

Ese fragmento no pertenece a PetMatch. Su propósito es comprobar que comprendiste la idea y puedes reconocerla fuera del ejemplo principal.

> [!WARNING]
> No copies pseudocódigo o ejemplos conceptuales esperando que formen una aplicación completa. Úsalos para entender una responsabilidad concreta.

---

## Cómo leer las rutas

Una **ruta de archivo**:

```text
src/main/java/com/petmatch/community/model/Pet.java
```

es diferente de una **ruta HTTP**:

```text
/pets/15
```

y ambas son diferentes de un endpoint REST como:

```text
/api/v1/pets/15
```

Durante el libro distinguiremos explícitamente estas ideas.

---

## Los nombres reales del código se conservan en inglés

PetMatch utiliza nombres como:

```text
SupportRequest
SupportApplication
PetService
SupportRequestRepository
findByIdAndOwnerId
```

Las explicaciones estarán en español, pero los nombres reales del código no se traducirán.

Por ejemplo:

> `SupportRequest` representa una solicitud de apoyo.

No inventaremos una clase `SolicitudApoyo` para explicar una clase que en el proyecto se llama `SupportRequest`.

En tu propio proyecto aplica la misma regla: **elige nombres coherentes con tu dominio y mantenlos consistentes en el código**.

---

## Aprende a reconocer responsabilidades

A lo largo del recorrido construirás un vocabulario técnico que debe servirte fuera de PetMatch.

Una primera aproximación será:

```text
Controller
recibe y coordina solicitudes HTTP.

Service
coordina casos de uso y protege reglas de negocio.

Repository
proporciona acceso a persistencia.

Entity
representa información persistente del dominio.

Form DTO
representa datos permitidos en una interacción de formulario.

API DTO
representa el contrato de entrada o salida de una API.

View
presenta información al usuario.
```

No memorices estas frases todavía. Cada capítulo mostrará evidencia concreta para construirlas.

---

## Leer código también es programar

Una habilidad fundamental de un desarrollador es navegar una aplicación que no escribió.

Encontrarás actividades como:

### 🛠 Prueba en el código

- localiza `PetService`;
- identifica sus dependencias en el constructor;
- busca `findOwnedPet(...)`;
- observa qué Repository utiliza;
- identifica qué excepción aparece cuando la mascota no pertenece al usuario.

Después, cuando tenga sentido, aparecerá una segunda parte:

### 🔁 Llévalo a tu proyecto

- localiza un caso de uso equivalente;
- identifica qué datos necesita;
- decide qué responsabilidad pertenece al Service;
- evita introducir lógica de negocio en la View o en código de configuración.

La combinación **leer + transferir** es más valiosa que copiar una solución completa.

---

## Qué significan las notas visuales

> [!NOTE]
> Aporta contexto o una aclaración.

> [!TIP]
> Sugiere una técnica útil de estudio o desarrollo.

> [!IMPORTANT]
> Marca una idea que tendrá consecuencias más adelante.

> [!WARNING]
> Señala un error frecuente, un riesgo o una interpretación incorrecta.

También encontrarás encabezados como:

### 🧠 Idea mental

Una representación sencilla antes de la definición técnica.

### 🔎 En PetMatch

La clase, método, configuración o template donde aparece el concepto.

### 🔁 Llévalo a tu proyecto

Una forma de reproducir o adaptar el concepto fuera de PetMatch.

### ⚠️ Error frecuente

Una equivocación común al comenzar.

### ✅ Qué debes recordar

Las ideas esenciales del capítulo.

---

## No todo Spring aparece en PetMatch

Spring es un ecosistema amplio. PetMatch utiliza solamente las capacidades necesarias para su alcance.

Por ejemplo, la API actual usa:

```text
HTTP Basic
+
STATELESS
```

No utiliza JWT.

Tampoco implementa carga de imágenes, pagos, chat o microservicios.

Distingue siempre estas tres categorías:

```text
Spring puede hacerlo
≠
PetMatch lo implementa
≠
tu proyecto lo necesita
```

La tercera pregunta es especialmente importante: una tecnología no debe agregarse solo porque aparezca en otro proyecto.

---

## Ejecuta la aplicación cuando el tema lo permita

El README principal explica cómo configurar:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

y cómo iniciar PetMatch con Maven Wrapper.

En Linux/macOS/WSL encontrarás comandos como:

```bash
./mvnw spring-boot:run
```

En Windows PowerShell o CMD se utiliza el wrapper para Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Lo mismo aplica a otros objetivos de Maven que veremos después.

> [!TIP]
> No memorices dos herramientas diferentes. `mvnw` y `mvnw.cmd` son dos scripts del mismo Maven Wrapper para plataformas distintas.

---

## El proyecto propio empieza pequeño

No intentes crear desde el primer día todas las capas, entidades, DTO, seguridad y endpoints de tu aplicación.

Un recorrido más útil es:

```text
1. crear proyecto Spring Boot
2. comprobar que arranca
3. entender estructura y Maven
4. crear una primera responsabilidad
5. separar Controller / Service cuando exista una razón
6. agregar persistencia cuando el dominio la necesite
7. agregar validación y seguridad sobre casos reales
```

En los capítulos 03–08 practicaremos precisamente la base de este recorrido.

Cuando termines el libro y las prácticas guiadas, ese proyecto pequeño deja de ser solo un espacio de ejercicios. Usa la ruta:

**[Construye tu propia aplicación con Spring Boot →](construye-tu-propia-aplicacion.md)**

Allí tendrás que elegir un dominio propio y recorrer desde el problema hasta testing y Git sin recibir las clases del proyecto como plantilla.

---

## Una estrategia de estudio por capítulo

### 1. Comprende el problema

No avances si todavía no sabes por qué el concepto es necesario.

### 2. Localízalo en PetMatch

Abre las rutas indicadas.

### 3. Explícalo sin leer

Intenta describir la responsabilidad con tus palabras.

### 4. Reproduce una versión pequeña

Solo cuando el capítulo lo proponga.

### 5. Compara

Pregunta:

```text
¿qué es igual conceptualmente?
¿qué cambia por el dominio?
¿qué parte pertenece a Spring?
¿qué parte pertenece al negocio?
```

### 6. Continúa

No necesitas dominar un concepto avanzado en una sola lectura. Necesitas entenderlo lo suficiente para reconocerlo cuando vuelva a aparecer.

---

## Si eres instructor

Una dinámica útil es pedir al aprendiz que prediga una responsabilidad antes de mostrar la implementación.

Ejemplos:

- “¿Dónde debería vivir la regla que impide eliminar una mascota con solicitudes?”
- “¿Ocultar un botón protege realmente un recurso?”
- “¿Quién debería decidir que aceptar una postulación cambia varios estados?”
- “¿Qué pieza de tu proyecto cumple una responsabilidad equivalente?”

La respuesta correcta importa, pero la justificación es todavía más importante.

---

## Antes de continuar

No necesitas conocer Spring todavía.

Lleva contigo cuatro preguntas:

1. **¿qué problema estamos resolviendo?**
2. **¿qué responsabilidad tiene cada pieza?**
3. **¿qué parte es del framework y qué parte pertenece al dominio?**
4. **¿podría reconocer esta misma idea en un proyecto diferente?**

El bloque de Fundamentos construirá esas respuestas paso a paso.

---

## Continúa con

- [Portada e índice](README.md)
- [Bloque 01 — Fundamentos](01-fundamentos/README.md)
- [Capítulo 01 — PetMatch y el problema](01-fundamentos/01-petmatch-y-el-problema.md)
- [Ruta autónoma — Construye tu propia aplicación](construye-tu-propia-aplicacion.md)

---

[← Portada](README.md) · [Índice](README.md) · [Siguiente → Capítulo 01](01-fundamentos/01-petmatch-y-el-problema.md) · [Ruta autónoma →](construye-tu-propia-aplicacion.md)
