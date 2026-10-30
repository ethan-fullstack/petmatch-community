# Práctica de transferencia — Web MVC con Thymeleaf

Esta práctica acompaña los capítulos 18–21 y continúa `BookShelf`, el proyecto secundario utilizado para demostrar transferencia.

Hasta este punto `BookShelf` ya tiene:

```text
Spring Boot
→ MySQL
→ JPA/Hibernate
→ Entities
→ Repositories
→ Services
→ transacciones
→ estados
→ locking
→ LAZY + EntityGraph
```

Ahora vamos a agregar una interfaz web server-side.

La pregunta central es:

> **¿Puedes convertir un caso de uso persistente en un recorrido HTTP → Controller → Service → Model → Thymeleaf → HTML sin copiar los Controllers de PetMatch?**

---

## Qué vas a construir

Implementaremos una interfaz para libros con estas rutas:

```text
GET  /books
GET  /books/new
POST /books
GET  /books/{id}
GET  /books/{id}/edit
POST /books/{id}
```

El usuario podrá:

```text
listar libros
→ ver detalle
→ abrir formulario
→ crear libro
→ editar libro
→ recibir errores de validación
```

Todavía **no** agregaremos autenticación ni autorización. Eso se hará en la práctica de Seguridad.

---

# 1. Relación con los capítulos 18–21

| Capítulo | Concepto | Aplicación en `BookShelf` |
|---|---|---|
| 18 | Spring MVC | rutas, Controller, Model, redirect y flash attributes |
| 19 | Thymeleaf | templates, expresiones, iteración, enlaces y fragments |
| 20 | Form DTO | `BookForm`, data binding, creación y edición |
| 21 | validación | Bean Validation, `@Valid`, `BindingResult`, errores de field |

La práctica no reemplaza esos capítulos. Sirve para demostrar que el mecanismo puede reproducirse en otro dominio.

---

# 2. Comprueba el proyecto antes de modificarlo

Ejecuta:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

Y comprueba que MySQL y las variables de entorno siguen disponibles.

No conviertas un error previo de persistencia en un supuesto error MVC.

---

# 3. Dependencias necesarias

En `pom.xml` necesitas las capacidades web, Thymeleaf y Validation.

Si generaste `BookShelf` con Web MVC desde la práctica de Fundamentos, esta dependencia ya debería existir:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Agrega Thymeleaf si todavía no está presente:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Y Validation:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Después ejecuta:

```bash
./mvnw test
```

La presencia de una dependencia no crea tus páginas ni tus reglas. Solo hace disponible la capacidad técnica.

---

# 4. Diseña primero el recorrido HTTP

Antes de escribir el Controller, escribe qué quieres que ocurra.

## Listar

```text
GET /books
→ obtener libros
→ ponerlos en Model
→ renderizar books/list
```

## Crear

```text
GET /books/new
→ preparar BookForm vacío
→ cargar categorías
→ renderizar books/form
```

```text
POST /books
→ data binding
→ validar BookForm
→ si hay errores: volver a books/form
→ si es válido: llamar BookService
→ redirect al detalle
```

## Editar

```text
GET /books/{id}/edit
→ recuperar Book
→ convertir Book a BookForm
→ cargar categorías
→ renderizar books/form
```

```text
POST /books/{id}
→ validar BookForm
→ actualizar mediante Service
→ redirect al detalle
```

Esto es más útil que comenzar copiando anotaciones.

---

# 5. Crea un Form DTO

Ruta sugerida:

```text
src/main/java/com/example/bookshelf/dto/book/BookForm.java
```

```java
package com.example.bookshelf.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookForm {

    @NotBlank
    @Size(max = 180)
    private String title;

    @NotBlank
    @Size(max = 32)
    private String isbn;

    @NotNull
    private Long categoryId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
```

Observa qué **no** contiene:

```text
id
status
loans
```

El formulario transporta los datos que el usuario puede editar en esta pantalla.

No es una copia automática de `Book`.

---

# 6. Entity y Form DTO tienen responsabilidades diferentes

## `Book`

Representa estado persistente del dominio:

```text
id
title
isbn
status
category
```

## `BookForm`

Representa entrada de una operación web:

```text
title
isbn
categoryId
```

La interfaz no puede enviar libremente:

```text
status = LOANED
```

porque ese estado pertenece al flujo de préstamos, no al formulario de edición del libro.

Esta separación evita que el contrato HTTP controle campos que no debería controlar.

---

# 7. Agrega una operación de actualización a `Book`

Una opción sencilla es mantener una operación pequeña en la Entity:

```java
public void updateDetails(
    String title,
    String isbn,
    Category category
) {
    this.title = title;
    this.isbn = isbn;
    this.category = category;
}
```

No cambia `status`.

El estado de préstamo sigue controlado por los casos de uso correspondientes.

---

# 8. Prepara consultas para la vista

Con `open-in-view: false`, la lista de libros no debería depender de cargar `category` durante el renderizado.

Puedes ampliar `BookRepository`:

```java
@EntityGraph(attributePaths = "category")
List<Book> findAllByOrderByTitleAsc();
```

Y para detalle:

```java
@EntityGraph(attributePaths = "category")
@Query("select b from Book b where b.id = :bookId")
Optional<Book> findDetailedById(@Param("bookId") Long bookId);
```

La vista podrá navegar:

```text
book.category.name
```

porque el caso de lectura preparó esa relación antes de terminar la transacción.

---

# 9. Crea `CategoryService`

El Controller no necesita consultar el Repository directamente.

Ruta:

```text
src/main/java/com/example/bookshelf/service/CategoryService.java
```

```java
package com.example.bookshelf.service;

import com.example.bookshelf.exception.ResourceNotFoundException;
import com.example.bookshelf.model.Category;
import com.example.bookshelf.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found: " + categoryId
            ));
    }
}
```

---

# 10. Crea una excepción para ISBN duplicado

Ruta:

```text
src/main/java/com/example/bookshelf/exception/DuplicateIsbnException.java
```

```java
package com.example.bookshelf.exception;

public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("ISBN already exists: " + isbn);
    }
}
```

¿Por qué no basta `@NotBlank`?

Porque:

```text
@NotBlank
→ valida estructura del campo

ISBN no duplicado
→ requiere consultar estado persistente
```

Son problemas diferentes.

---

# 11. Amplía `BookRepository`

Necesitamos comprobar duplicados durante creación y edición.

```java
boolean existsByIsbn(String isbn);

boolean existsByIsbnAndIdNot(String isbn, Long id);
```

Ahora Repository responde preguntas de persistencia.

El Service decidirá qué hacer con esas respuestas.

---

# 12. Crea `BookService`

Ruta:

```text
src/main/java/com/example/bookshelf/service/BookService.java
```

```java
package com.example.bookshelf.service;

import com.example.bookshelf.dto.book.BookForm;
import com.example.bookshelf.exception.DuplicateIsbnException;
import com.example.bookshelf.exception.ResourceNotFoundException;
import com.example.bookshelf.model.Book;
import com.example.bookshelf.model.Category;
import com.example.bookshelf.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryService categoryService;

    public BookService(
        BookRepository bookRepository,
        CategoryService categoryService
    ) {
        this.bookRepository = bookRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAllByOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public Book findById(Long bookId) {
        return bookRepository.findDetailedById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Book not found: " + bookId
            ));
    }

    @Transactional
    public Book create(BookForm form) {
        String isbn = form.getIsbn().trim();

        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        Category category = categoryService.findById(
            form.getCategoryId()
        );

        Book book = new Book(
            form.getTitle().trim(),
            isbn,
            category
        );

        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long bookId, BookForm form) {
        Book book = findById(bookId);
        String isbn = form.getIsbn().trim();

        if (bookRepository.existsByIsbnAndIdNot(isbn, bookId)) {
            throw new DuplicateIsbnException(isbn);
        }

        Category category = categoryService.findById(
            form.getCategoryId()
        );

        book.updateDetails(
            form.getTitle().trim(),
            isbn,
            category
        );

        return book;
    }

    public BookForm toForm(Book book) {
        BookForm form = new BookForm();
        form.setTitle(book.getTitle());
        form.setIsbn(book.getIsbn());
        form.setCategoryId(book.getCategory().getId());
        return form;
    }
}
```

---

# 13. ¿Por qué `toForm(...)`?

Para editar, la pantalla necesita un DTO inicializado.

```text
Book persistente
→ BookForm
→ campos HTML
```

No significa que Entity y DTO sean equivalentes.

`toForm(...)` traduce de un modelo a otro para un caso de uso específico.

---

# 14. Crea `BookController`

Ruta:

```text
src/main/java/com/example/bookshelf/controller/BookController.java
```

Empieza con la estructura:

```java
package com.example.bookshelf.controller;

import com.example.bookshelf.dto.book.BookForm;
import com.example.bookshelf.exception.DuplicateIsbnException;
import com.example.bookshelf.model.Book;
import com.example.bookshelf.service.BookService;
import com.example.bookshelf.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    public BookController(
        BookService bookService,
        CategoryService categoryService
    ) {
        this.bookService = bookService;
        this.categoryService = categoryService;
    }
}
```

El Controller depende de Services, no de JPA directamente.

---

# 15. Lista de libros

Agrega:

```java
@GetMapping
public String list(Model model) {
    model.addAttribute("books", bookService.findAll());
    return "books/list";
}
```

Lectura:

```text
GET /books
→ BookController.list
→ BookService.findAll
→ Model["books"]
→ books/list.html
```

`return "books/list"` no significa que el navegador reciba el texto `books/list`.

Es un **nombre lógico de vista**.

---

# 16. Formulario de creación

```java
@GetMapping("/new")
public String createForm(Model model) {
    model.addAttribute("bookForm", new BookForm());
    model.addAttribute("categories", categoryService.findAll());
    model.addAttribute("editing", false);
    return "books/form";
}
```

La vista necesita más de un dato:

```text
bookForm
categories
editing
```

El `Model` funciona como el conjunto de datos que el Controller entrega a la vista.

---

# 17. Procesa creación

```java
@PostMapping
public String create(
    @Valid @ModelAttribute("bookForm") BookForm form,
    BindingResult bindingResult,
    Model model,
    RedirectAttributes redirectAttributes
) {
    if (bindingResult.hasErrors()) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("editing", false);
        return "books/form";
    }

    try {
        Book book = bookService.create(form);
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Libro registrado correctamente."
        );
        return "redirect:/books/" + book.getId();
    } catch (DuplicateIsbnException exception) {
        bindingResult.rejectValue(
            "isbn",
            "isbn.duplicate",
            "Ya existe un libro con este ISBN."
        );
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("editing", false);
        return "books/form";
    }
}
```

---

# 18. El orden de `BindingResult` importa

Observa:

```java
@Valid @ModelAttribute("bookForm") BookForm form,
BindingResult bindingResult
```

`BindingResult` está inmediatamente después del objeto validado.

Conceptualmente:

```text
HTTP fields
→ data binding
→ BookForm
→ Bean Validation
→ BindingResult
```

El Controller puede decidir si vuelve a renderizar o continúa hacia el Service.

---

# 19. Error estructural vs error de negocio

Ejemplos:

## Bean Validation

```text
title vacío
isbn vacío
categoryId null
```

Se detectan con annotations del DTO.

## Regla que consulta persistencia

```text
ISBN duplicado
```

La detecta el Service y el Controller la adapta a un error del field:

```java
bindingResult.rejectValue(...)
```

No conviertas todos los errores de negocio en annotations del DTO.

---

# 20. Detalle

```java
@GetMapping("/{bookId}")
public String detail(
    @PathVariable Long bookId,
    Model model
) {
    model.addAttribute("book", bookService.findById(bookId));
    return "books/detail";
}
```

Aquí Spring convierte el segmento de URL a `Long`.

```text
/books/15
→ bookId = 15L
```

Eso es binding de entrada HTTP, no una comprobación de existencia.

La existencia se valida en Service/Repository.

---

# 21. Formulario de edición

```java
@GetMapping("/{bookId}/edit")
public String editForm(
    @PathVariable Long bookId,
    Model model
) {
    Book book = bookService.findById(bookId);

    model.addAttribute("bookForm", bookService.toForm(book));
    model.addAttribute("categories", categoryService.findAll());
    model.addAttribute("editing", true);
    model.addAttribute("bookId", bookId);

    return "books/form";
}
```

El flujo es:

```text
Book
→ toForm
→ BookForm
→ HTML prellenado
```

---

# 22. Procesa edición

```java
@PostMapping("/{bookId}")
public String update(
    @PathVariable Long bookId,
    @Valid @ModelAttribute("bookForm") BookForm form,
    BindingResult bindingResult,
    Model model,
    RedirectAttributes redirectAttributes
) {
    if (bindingResult.hasErrors()) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("editing", true);
        model.addAttribute("bookId", bookId);
        return "books/form";
    }

    try {
        bookService.update(bookId, form);
        redirectAttributes.addFlashAttribute(
            "successMessage",
            "Libro actualizado correctamente."
        );
        return "redirect:/books/" + bookId;
    } catch (DuplicateIsbnException exception) {
        bindingResult.rejectValue(
            "isbn",
            "isbn.duplicate",
            "Ya existe un libro con este ISBN."
        );
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("editing", true);
        model.addAttribute("bookId", bookId);
        return "books/form";
    }
}
```

---

# 23. Render vs redirect

Cuando hay error:

```java
return "books/form";
```

El servidor vuelve a **renderizar** la plantilla en la misma request.

Cuando la operación termina correctamente:

```java
return "redirect:/books/" + bookId;
```

el navegador recibe una redirección y realiza una nueva request.

Ese patrón evita reenviar accidentalmente el mismo POST al refrescar una página de éxito.

---

# 24. Flash attributes

```java
redirectAttributes.addFlashAttribute(
    "successMessage",
    "Libro actualizado correctamente."
);
```

El mensaje está pensado para sobrevivir a la redirección y estar disponible en la siguiente request.

No lo confundas con:

```java
model.addAttribute(...)
```

que pertenece al renderizado de la request actual.

---

# 25. Crea la estructura de templates

```text
src/main/resources/templates/
├── fragments/
│   ├── head.html
│   ├── navigation.html
│   └── alerts.html
├── books/
│   ├── list.html
│   ├── detail.html
│   └── form.html
└── home.html
```

No es una obligación universal de Thymeleaf. Es una organización útil para este proyecto.

---

# 26. Fragment `head`

```html
<!doctype html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title th:text="${title}">BookShelf</title>
</head>
</html>
```

Después una página puede reutilizarlo con:

```html
<head th:replace="~{fragments/head :: head('Libros')}"></head>
```

El objetivo del fragment es evitar repetir estructura compartida.

---

# 27. Navegación

```html
<nav th:fragment="navigation">
    <a th:href="@{/}">Inicio</a>
    <a th:href="@{/books}">Libros</a>
    <a th:href="@{/books/new}">Nuevo libro</a>
</nav>
```

`@{...}` representa construcción de URLs dentro de Thymeleaf.

No escribas rutas dinámicas concatenando strings HTML manualmente si Thymeleaf puede construirlas.

---

# 28. Alertas

```html
<div th:fragment="alerts">
    <p
        th:if="${successMessage}"
        th:text="${successMessage}"
    ></p>
</div>
```

Después:

```html
<div th:replace="~{fragments/alerts :: alerts}"></div>
```

---

# 29. `books/list.html`

```html
<!doctype html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/head :: head('Libros')}"></head>
<body>
    <nav th:replace="~{fragments/navigation :: navigation}"></nav>
    <div th:replace="~{fragments/alerts :: alerts}"></div>

    <main>
        <h1>Libros</h1>

        <p th:if="${#lists.isEmpty(books)}">
            No hay libros registrados.
        </p>

        <article th:each="book : ${books}">
            <h2>
                <a
                    th:href="@{/books/{id}(id=${book.id})}"
                    th:text="${book.title}"
                ></a>
            </h2>
            <p th:text="${book.isbn}"></p>
            <p th:text="${book.category.name}"></p>
            <p th:text="${book.status}"></p>
        </article>
    </main>
</body>
</html>
```

Conceptos:

```text
${books}
→ dato del Model

th:each
→ iteración

th:text
→ salida escapada

th:href
→ URL dinámica
```

---

# 30. `books/detail.html`

```html
<!doctype html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/head :: head('Detalle del libro')}"></head>
<body>
    <nav th:replace="~{fragments/navigation :: navigation}"></nav>
    <div th:replace="~{fragments/alerts :: alerts}"></div>

    <main>
        <h1 th:text="${book.title}"></h1>
        <p th:text="${book.isbn}"></p>
        <p th:text="${book.category.name}"></p>
        <p th:text="${book.status}"></p>

        <a
            th:href="@{/books/{id}/edit(id=${book.id})}"
        >Editar</a>
    </main>
</body>
</html>
```

La vista navega `book.category.name` porque el caso de lectura preparó `category`.

No depende de una carga LAZY tardía durante el render.

---

# 31. `books/form.html`

```html
<!doctype html>
<html lang="es" xmlns:th="http://www.thymeleaf.org">
<head
    th:replace="~{fragments/head :: head(${editing} ? 'Editar libro' : 'Nuevo libro')}"
></head>
<body>
    <nav th:replace="~{fragments/navigation :: navigation}"></nav>

    <main>
        <h1 th:text="${editing} ? 'Editar libro' : 'Nuevo libro'"></h1>

        <form
            th:object="${bookForm}"
            th:action="${editing}
                ? @{/books/{id}(id=${bookId})}
                : @{/books}"
            method="post"
        >
            <div>
                <label for="title">Título</label>
                <input id="title" type="text" th:field="*{title}">
                <p th:errors="*{title}"></p>
            </div>

            <div>
                <label for="isbn">ISBN</label>
                <input id="isbn" type="text" th:field="*{isbn}">
                <p th:errors="*{isbn}"></p>
            </div>

            <div>
                <label for="categoryId">Categoría</label>
                <select id="categoryId" th:field="*{categoryId}">
                    <option value="">Selecciona una categoría</option>
                    <option
                        th:each="category : ${categories}"
                        th:value="${category.id}"
                        th:text="${category.name}"
                    ></option>
                </select>
                <p th:errors="*{categoryId}"></p>
            </div>

            <button type="submit">Guardar</button>
        </form>
    </main>
</body>
</html>
```

---

# 32. `${...}` y `*{...}` no son lo mismo

En:

```html
<form th:object="${bookForm}">
```

`bookForm` se obtiene del Model.

Dentro del formulario:

```html
th:field="*{title}"
```

`*{title}` se evalúa respecto al objeto seleccionado por `th:object`.

Modelo mental:

```text
${bookForm}
→ buscar objeto en el contexto

*{title}
→ propiedad del objeto seleccionado
```

---

# 33. Data binding

Si el navegador envía:

```text
title=Spring práctico
isbn=9780000000002
categoryId=3
```

Spring intenta construir conceptualmente:

```java
BookForm form = new BookForm();
form.setTitle("Spring práctico");
form.setIsbn("9780000000002");
form.setCategoryId(3L);
```

Eso es **data binding**.

No significa que exista `Category 3`.

Solo significa que el texto HTTP pudo convertirse a `Long`.

La existencia se comprueba después en el Service.

---

# 34. ¿Qué ocurre con un valor imposible de convertir?

Si llega algo equivalente a:

```text
categoryId=abc
```

Spring no puede convertir `abc` a `Long`.

Ese es un error de binding/tipo.

No es lo mismo que:

```text
categoryId=9999
```

que sí es un `Long`, pero puede apuntar a una categoría inexistente.

Distingue:

```text
conversión
≠
validación estructural
≠
existencia
≠
regla de negocio
```

---

# 35. Validación por capas

Para crear un libro pueden participar varias capas.

## HTML

Puede ayudar al usuario con:

```html
required
maxlength
```

## Bean Validation

Protege el DTO en backend:

```text
@NotBlank
@NotNull
@Size
```

## Service

Protege reglas:

```text
ISBN no duplicado
Category debe existir
```

## Base de datos

Puede tener constraints:

```text
NOT NULL
UNIQUE
foreign key
```

Ninguna capa vuelve innecesarias automáticamente a las demás.

---

# 36. La vista no debe ser la única regla

Podrías ocultar el botón “Editar” cuando un libro esté prestado.

Eso sería una decisión visual.

Pero si la operación estuviera prohibida en ese estado, la regla debería verificarse también en backend.

```text
th:if
≠
regla de seguridad o negocio definitiva
```

Esta idea será todavía más importante en el bloque de Seguridad.

---

# 37. Agrega datos iniciales para categorías

La pantalla necesita categorías existentes.

Para una práctica local puedes insertarlas manualmente:

```sql
INSERT INTO categories (name) VALUES
('Backend'),
('Bases de datos'),
('Arquitectura');
```

No conviertas este SQL manual en una supuesta estrategia de migraciones.

Es solo dato de apoyo para la práctica.

---

# 38. Arranca la aplicación

```bash
./mvnw spring-boot:run
```

Abre:

```text
http://localhost:8080/books
```

O el puerto que hayas configurado en tu proyecto.

---

# 39. Verificación 1 — Lista vacía

Con una base sin libros:

```text
GET /books
→ 200
→ template renderizado
→ mensaje “No hay libros registrados”
```

Si ves un error relacionado con `book.category.name`, revisa el fetch plan antes de cambiar todo a EAGER.

---

# 40. Verificación 2 — Error de formulario

Abre:

```text
/books/new
```

Envía el formulario vacío.

Debes observar errores de:

```text
title
isbn
categoryId
```

El flujo esperado es:

```text
POST /books
→ BookForm
→ @Valid
→ BindingResult con errores
→ render books/form
```

No debe invocarse `bookService.create(...)` si ya existen errores de entrada.

---

# 41. Verificación 3 — Creación válida

Envía:

```text
title = Effective Java
isbn = 9780134685991
category = una existente
```

Resultado:

```text
POST /books
→ BookService.create
→ INSERT
→ redirect:/books/{id}
→ GET detalle
→ successMessage
```

Observa la URL final: debe ser una nueva GET después del POST.

---

# 42. Verificación 4 — ISBN duplicado

Intenta crear otro libro con el mismo ISBN.

Esperado:

```text
BookService
→ detecta duplicado
→ DuplicateIsbnException
→ Controller
→ bindingResult.rejectValue("isbn", ...)
→ books/form
```

El error de negocio se adapta a la interfaz web sin mover la regla al Controller.

---

# 43. Verificación 5 — Edición

Abre:

```text
/books/{id}/edit
```

Debes ver campos prellenados.

Cambia título o categoría y guarda.

Esperado:

```text
Book
→ BookForm
→ edición HTML
→ POST
→ BookService.update
→ dirty checking
→ redirect detalle
```

---

# 44. Error frecuente — devolver HTML desde el Service

No hagas:

```java
public String create(...) {
    return "books/detail";
}
```

si `BookService` representa negocio.

El Service no debería decidir qué template renderiza la aplicación.

```text
Service
→ caso de uso

Controller
→ HTTP + vista/redirect
```

---

# 45. Error frecuente — Repository en Controller

Evita:

```java
@Controller
public class BookController {
    private final BookRepository bookRepository;
}
```

cuando la operación tiene reglas y una capa Service ya definida.

El Controller no necesita conocer cómo se persiste cada dato.

---

# 46. Error frecuente — Entity como formulario universal

Evita asumir:

```java
@ModelAttribute Book book
```

como solución automática para cualquier formulario.

Eso puede exponer campos persistentes que la pantalla no debería controlar.

El Form DTO permite declarar el contrato de entrada de la operación.

---

# 47. Error frecuente — olvidar reconstruir el Model cuando hay errores

Si `books/form.html` necesita:

```text
categories
editing
bookId
```

entonces al volver a renderizar por error debes volver a proporcionar esos datos.

De lo contrario el template puede fallar aunque `BindingResult` esté funcionando correctamente.

---

# 48. Error frecuente — confundir redirect con template

```java
return "books/list";
```

significa renderizar una vista.

```java
return "redirect:/books";
```

significa responder con redirección.

No son dos formas equivalentes de “ir a una página”.

---

# 49. Checkpoint 18 — Spring MVC

Sin mirar código, explica:

```text
GET /books
→ ?
→ ?
→ ?
→ HTML
```

Debes incluir:

```text
DispatcherServlet conceptualmente
Controller
Service
Model
view name
Thymeleaf
```

Revisa [capítulo 18](18-spring-mvc.md) si no puedes hacerlo.

---

# 50. Checkpoint 19 — Thymeleaf

Explica qué hace cada expresión:

```html
th:text="${book.title}"
th:each="book : ${books}"
th:href="@{/books/{id}(id=${book.id})}"
th:replace="~{fragments/navigation :: navigation}"
```

Y distingue:

```text
${...}
*{...}
@{...}
```

Revisa [capítulo 19](19-thymeleaf.md).

---

# 51. Checkpoint 20 — Form DTO

Responde:

```text
¿Por qué BookForm tiene categoryId y Book tiene Category?
¿Por qué BookForm no expone status?
¿Qué hace toForm(...) durante edición?
¿Qué significa data binding?
```

Revisa [capítulo 20](20-formularios-y-form-dto.md).

---

# 52. Checkpoint 21 — Validación

Clasifica:

```text
title vacío
→ Bean Validation

categoryId = abc
→ binding/conversión

categoryId válido pero inexistente
→ Service/persistencia

ISBN duplicado
→ regla + persistencia

UNIQUE isbn
→ constraint DB
```

Revisa [capítulo 21](21-validacion.md).

---

# 53. Prueba de transferencia final

Sin abrir PetMatch, dibuja el flujo de creación:

```text
Browser
→ POST /books
→ BookController
→ BookForm
→ @Valid
→ BindingResult
→ BookService
→ BookRepository
→ MySQL
→ redirect
→ GET /books/{id}
→ Model
→ Thymeleaf
→ HTML
```

Después explica dónde cambiarías el sistema si quisieras:

```text
cambiar HTML
cambiar regla de ISBN
añadir otro campo de formulario
cambiar query de listado
cambiar mensaje de éxito
```

Si puedes ubicar cada cambio en la capa correcta, ya no estás copiando MVC: estás comprendiendo responsabilidades.

---

# 54. Checklist de salida

Antes de pasar a Seguridad, comprueba que puedes:

- [ ] crear un `@Controller`;
- [ ] combinar `@RequestMapping`, `@GetMapping` y `@PostMapping`;
- [ ] usar `@PathVariable`;
- [ ] poner datos en `Model`;
- [ ] devolver un nombre lógico de vista;
- [ ] distinguir render de redirect;
- [ ] usar flash attributes;
- [ ] crear un Form DTO distinto de Entity;
- [ ] usar `@ModelAttribute`;
- [ ] explicar data binding;
- [ ] convertir Entity → Form DTO para edición;
- [ ] usar `@Valid` y `BindingResult`;
- [ ] mostrar errores con `th:errors`;
- [ ] usar `th:object` y `th:field`;
- [ ] usar `${...}`, `*{...}` y `@{...}`;
- [ ] iterar con `th:each`;
- [ ] reutilizar fragments;
- [ ] mantener reglas en Service;
- [ ] preparar relaciones antes de Thymeleaf con `open-in-view: false`;
- [ ] ejecutar el flujo completo desde navegador hasta MySQL.

---

## Resultado esperado

Al finalizar esta práctica, una pantalla HTML ya no debería parecer una pieza aislada.

Deberías verla como el final de un flujo:

```text
HTTP
→ MVC
→ binding
→ validation
→ Service
→ persistencia
→ Model
→ Thymeleaf
→ HTML
```

Y deberías poder construir ese recorrido en un dominio distinto de mascotas.

---

## Continúa con

Después continúa con:

**[Bloque 04 — Seguridad →](../04-seguridad/README.md)**

---

[← Índice Web MVC](README.md) · [Índice general](../README.md) · [Siguiente bloque → Seguridad](../04-seguridad/README.md)
