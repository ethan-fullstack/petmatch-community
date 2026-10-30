# 43 — Usabilidad, navegación y accesibilidad: una interfaz que se puede comprender y utilizar

En el [capítulo 40](40-roles-autorizacion-y-auditoria.md) protegimos las operaciones administrativas. En el [capítulo 41](41-reportes-dinamicos-jpa.md) aprendimos a presentar resultados filtrados, y en el [capítulo 42](42-exportacion-reportes-pdf-excel-strategy.md) permitimos descargarlos. Sin embargo, una aplicación puede cumplir todas esas reglas técnicas y seguir siendo difícil de utilizar.

> **¿Puede una persona completar una tarea, identificar dónde está, comprender un error y manejar los controles sin depender exclusivamente de un ratón o de una presentación visual concreta?**

Esa pregunta nos lleva a tres conceptos que se relacionan, pero **no son sinónimos**: usabilidad, navegación y accesibilidad.

PetMatch Community será nuestro caso de estudio. Revisaremos las plantillas Thymeleaf, los estilos CSS y los pequeños scripts JavaScript que ya existen. También analizaremos qué comprueban las pruebas automatizadas y qué evidencias todavía se necesitan. El objetivo no es copiar la apariencia de PetMatch: es aprender a justificar y comprobar decisiones de interacción en **cualquier aplicación web**.

Al terminar podrás:

- distinguir usabilidad, navegación, diseño adaptable y accesibilidad;
- diseñar recorridos completos alrededor de las tareas del usuario;
- explicar el papel del HTML semántico, etiquetas, mensajes de estado y errores de formulario;
- recorrer una interfaz con teclado, foco visible y enlaces para saltar al contenido;
- interpretar las decisiones actuales de navegación, tema claro/oscuro y diseño móvil;
- identificar límites de las comprobaciones automáticas y preparar evaluaciones con personas;
- convertir observaciones concretas en mejoras verificables y transferibles.

---

## 1. Una aplicación correcta no es necesariamente una aplicación fácil de usar

Imagina que una persona quiere registrar una mascota. El servidor valida los datos, persiste correctamente la entidad y redirige a la vista de detalle. Desde el punto de vista del código, la operación puede funcionar.

Pero todavía quedan preguntas importantes:

1. ¿La persona encontró el enlace para registrar la mascota?
2. ¿Sabía qué campos debía completar?
3. ¿Comprendió por qué se rechazó una edad negativa?
4. ¿Pudo identificar el control que debía corregir?
5. ¿Pudo completar la operación utilizando únicamente el teclado?
6. ¿Comprendió que el registro terminó correctamente?

Las pruebas de servicios y repositorios responden preguntas técnicas diferentes. No debemos usar una suite aprobada como prueba automática de que la experiencia completa es adecuada.

### Cuatro conceptos que conviene separar

| Concepto | Pregunta central | Ejemplo |
|---|---|---|
| **Usabilidad** | ¿Las personas pueden completar sus tareas de forma efectiva, eficiente y comprensible? | Registrar una mascota sin confundirse con los campos. |
| **Navegación** | ¿Pueden ubicarse y encontrar el siguiente paso? | Pasar de «Mis mascotas» a «Mis solicitudes». |
| **Accesibilidad** | ¿El contenido y los controles pueden percibirse, comprenderse y operarse con distintas capacidades y tecnologías? | Relacionar un mensaje de error con su campo. |
| **Diseño adaptable** | ¿La distribución funciona en pantallas y tamaños distintos? | Reorganizar filtros en una columna estrecha. |

El diseño adaptable puede contribuir a la accesibilidad, pero **no garantiza por sí mismo** que una página pueda utilizarse con teclado o lector de pantalla.

**🧠 Idea mental**

```text
Funcionalidad disponible
        ↓
Interfaz comprensible
        ↓
Tarea realizable
        ↓
Resultado perceptible
        ↓
Evaluación con criterios observables
```

**Antes de continuar:** escribe una diferencia entre «el formulario guarda» y «una persona puede completar correctamente el formulario». ¿Qué comprobaría cada afirmación?

---

## 2. Diseñar alrededor de tareas, no de pantallas aisladas

Una aplicación se comprende mejor a partir de recorridos de usuario.

En PetMatch, una persona puede:

```text
Registrarse
    ↓
Iniciar sesión
    ↓
Registrar mascota
    ↓
Crear solicitud de apoyo
    ↓
Consultar su estado
```

Otra persona puede:

```text
Iniciar sesión
    ↓
Consultar solicitudes abiertas
    ↓
Postularse
    ↓
Revisar el estado de su postulación
```

Una cuenta administradora tiene otro recorrido:

```text
Entrar al área de administración
    ↓
Consultar usuarios / cambiar rol
    ↓
Consultar reportes
    ↓
Aplicar filtros y navegar páginas
    ↓
Descargar PDF o Excel
```

Estos recorridos comparten componentes de navegación, pero no todas las personas tienen las mismas acciones. La aplicación también debe ser comprensible cuando una acción **no está permitida**, no existe todavía o requiere completar otra tarea.

### Una pantalla puede estar correcta y el recorrido incompleto

Supongamos que el formulario «Nueva solicitud» funciona, pero no explica que primero debes registrar una mascota. La persona puede llegar a un punto muerto.

**🔎 En PetMatch — [support-requests/form.html](../../src/main/resources/templates/support-requests/form.html):**

```html
<div th:if="${pets.isEmpty()}"
     class="mt-6 rounded-xl border border-amber-400/30 bg-amber-400/10 p-4 text-amber-100">
    Primero debes registrar una mascota.
    <a th:href="@{/pets/new}" class="font-bold underline">Crear mascota</a>
</div>
```

Este mensaje conecta el problema con una **siguiente acción concreta**. No obliga a la persona a adivinar por qué no puede continuar.

**Decisión transferible:** cuando falta un prerrequisito, explica qué falta y ofrece una ruta para resolverlo, en vez de mostrar solamente una pantalla vacía.

---

## 3. Estructura semántica: el HTML tiene significado

Antes de utilizar clases CSS o JavaScript, el documento necesita una estructura que comunique qué representa cada elemento.

Algunos elementos tienen responsabilidades específicas:

| Elemento | Función habitual |
|---|---|
| `<main>` | Contenido principal de la página |
| `<nav>` | Región de navegación |
| `<h1>` y otros encabezados | Organización jerárquica del contenido |
| `<form>` | Agrupación de controles que se envían |
| `<label>` | Nombre visible o programáticamente asociado a un control |
| `<button>` | Acción que se puede activar |
| `<a>` con `href` | Navegación hacia un recurso |
| `<table>` | Datos tabulares con filas y columnas |
| `<th scope="col">` | Encabezado de una columna |

Usar HTML semántico facilita el funcionamiento normal del navegador, el teclado y muchas tecnologías de apoyo.

### Código real: contenido principal y navegación

La vista administrativa [admin/users.html](../../src/main/resources/templates/admin/users.html) incluye:

```html
<a href="#main-content" class="skip-link">Saltar al contenido</a>

<main id="main-content" tabindex="-1" class="pm-shell">
    <nav th:replace="~{fragments/navigation :: appNavigation}"></nav>

    <header class="pm-admin-header">
        <div>
            <span class="pm-eyebrow">Panel de administración</span>
            <h1 class="pm-admin-title">Administrar usuarios</h1>
        </div>
    </header>
</main>
```

- El enlace inicial permite llegar al contenido sin recorrer toda la navegación.
- `main` identifica la región principal.
- `id="main-content"` permite utilizarla como destino.
- `tabindex="-1"` permite gestionar el foco sobre la región sin incluirla en la secuencia normal de Tab.
- `h1` comunica el tema principal de esa página.

No confundas agregar `role` o `aria-*` con crear semántica automáticamente. Cuando existe un elemento HTML nativo apropiado, normalmente debe ser la primera opción.

### Un error habitual

```html
<div onclick="saveChanges()">Guardar</div>
```

Este fragmento conceptual no tiene por sí solo la semántica y el comportamiento de teclado de un botón.

La alternativa nativa es:

```html
<button type="submit">Guardar</button>
```

**🔁 Llévalo a tu proyecto:** localiza una acción principal y comprueba si utiliza un botón o enlace adecuado, en vez de una etiqueta visual adaptada artificialmente.

---

## 4. Navegación compartida sin duplicar el menú en cada página

PetMatch utiliza Thymeleaf para reutilizar la navegación.

**Código real — [fragments/navigation.html](../../src/main/resources/templates/fragments/navigation.html):**

```html
<nav th:fragment="appNavigation"
     class="pm-nav"
     aria-label="Navegación principal">
    <div class="pm-nav__links">
        <a th:href="@{/pets}" class="pm-nav__link">Mis mascotas</a>
        <a th:href="@{/support-requests}" class="pm-nav__link">
            Solicitudes abiertas
        </a>
        <a th:href="@{/support-requests/mine}" class="pm-nav__link">
            Mis solicitudes
        </a>
        <a th:href="@{/support-applications/mine}" class="pm-nav__link">
            Mis postulaciones
        </a>
    </div>
</nav>
```

Las páginas lo incorporan mediante:

```html
<nav th:replace="~{fragments/navigation :: appNavigation}"></nav>
```

**¿Qué aporta la reutilización?**

- Las rutas y nombres de secciones se mantienen en un lugar común.
- Es más fácil corregir una etiqueta confusa.
- Disminuye el riesgo de que cada vista muestre una navegación diferente.
- Se conserva un modelo de navegación reconocible a lo largo de la aplicación.

### Visibilidad del menú y autorización

El fragmento usa `sec:authorize="hasRole('ADMIN')"` para los enlaces administrativos. Eso controla la **presentación**; la autorización real de `/admin/**` sigue en Spring Security.

Recuerda el [capítulo 40](40-roles-autorizacion-y-auditoria.md): **ocultar una opción no es proteger una operación**.

### ¿Por qué importa el nombre del enlace?

«Solicitudes abiertas» y «Mis solicitudes» se parecen, pero responden preguntas diferentes. La etiqueta orienta sobre el contenido que encontrará la persona.

**⚠️ Error frecuente:** llamar «Solicitudes» a todos los enlaces y obligar al usuario a entrar y salir de páginas para descubrir sus diferencias.

---

## 5. Indicar dónde está la persona: estado de navegación

Una navegación persistente debe comunicar la sección actual. El código incluye [navigation.js](../../src/main/resources/static/js/navigation.js), que examina la ruta actual y los enlaces del menú.

**Código real — fragmento:**

```javascript
const currentPath = window.location.pathname.replace(/\/+$/, "") || "/";
const links = Array.from(document.querySelectorAll(".pm-nav__link"));

const matching = links.filter((link) => {
    const target = new URL(link.href).pathname.replace(/\/+$/, "") || "/";
    return currentPath === target
        || (target !== "/" && currentPath.startsWith(target + "/"));
});

const selected = matching.sort(
    (first, second) => second.pathname.length - first.pathname.length
)[0];

if (selected) {
    selected.setAttribute("aria-current", "page");
    selected.classList.add("pm-nav__link--active");
}
```

### ¿Por qué buscar la coincidencia más específica?

Considera:

```text
/support-requests
/support-requests/mine
```

Una petición a `/support-requests/mine` comparte el prefijo del enlace más general. El script intenta elegir el enlace más específico cuando existen coincidencias.

Después agrega:

- `aria-current="page"`, que comunica a tecnologías compatibles cuál es el enlace actual;
- `.pm-nav__link--active`, que activa una presentación visual diferenciada.

**Código real — [application.css](../../src/main/resources/static/css/application.css):**

```css
.pm-nav__link--active {
    text-decoration: underline;
    text-underline-offset: .3em;
    background: #164e63;
    color: #f8fafc !important;
}
```

No depende **solo del color**: también hay subrayado.

### Límite que debes reconocer

El script aplica una aproximación basada en rutas. Una URL de detalle como `/support-requests/42` puede considerarse dentro de «Solicitudes abiertas» aunque el usuario haya llegado por otro recorrido.

Eso no constituye necesariamente un error técnico: revela que **la sección activa inferida de una URL no siempre expresa el contexto desde el cual llegó la persona**. Si el producto exige orientación más precisa, podrían introducirse migas de pan o información explícita del modelo de navegación.

**🧪 Antes de ejecutar:** ¿qué enlace seleccionaría el script para `/support-requests/mine` y por qué? ¿Qué supuesto hace para una URL de detalle?

---

## 6. El teclado es una forma principal de interacción

No todas las personas utilizan un ratón. Un diseño operable debe permitir llegar a los controles y utilizarlos mediante teclado, según la naturaleza de cada componente.

Controles nativos como enlaces, botones, entradas y selectores ya cuentan con comportamientos de teclado que no conviene reemplazar sin necesidad.

### El orden de enfoque

En una página con navegación y formulario, una secuencia razonable es:

```text
Saltar al contenido
    ↓
Enlaces de navegación
    ↓
Campos del formulario
    ↓
Botón principal
    ↓
Acción secundaria
```

El orden real depende del DOM y de los elementos interactivos disponibles. Debe comprobarse en el navegador; **no se demuestra mirando únicamente una captura de pantalla**.

### Foco visible

**Código real — [accessibility.css](../../src/main/resources/static/css/accessibility.css):**

```css
:focus-visible {
    outline: 3px solid #22d3ee !important;
    outline-offset: 3px;
}
```

Esta regla añade una indicación de foco cuando el navegador considera apropiado aplicar `:focus-visible`, especialmente en navegación por teclado.

Eliminar el contorno mediante `outline: none` sin proporcionar un reemplazo perceptible es un riesgo conocido. En PetMatch hay campos con esa clase, por lo que la regla compartida de `:focus-visible` cumple una función concreta.

### Saltar al contenido principal

**Código real — [accessibility.css](../../src/main/resources/static/css/accessibility.css):**

```css
.skip-link {
    position: absolute;
    left: -10000px;
}

.skip-link:focus {
    left: 1rem;
    top: 1rem;
    background: #0f172a;
    color: #fff;
    padding: 1rem;
    z-index: 100;
}
```

El enlace existe antes de la navegación, pero permanece visualmente fuera de la pantalla hasta recibir el foco.

**Prueba manual:**

1. Abre `/admin/reports` con una cuenta autorizada.
2. Presiona `Tab` desde el comienzo del documento.
3. Busca la opción «Saltar al contenido».
4. Presiona `Enter`.
5. Comprueba que el contenido principal se convierte en destino de navegación.
6. Continúa con `Tab` para localizar los controles.

El script de navegador de PetMatch contiene una comprobación mecánica de este recorrido, pero **ver un script escrito no demuestra que la prueba se haya ejecutado en la revisión actual**.

---

## 7. Formularios: una etiqueta no es un placeholder

Los formularios pueden fallar en usabilidad aunque validen correctamente los datos.

**Código real — [pets/form.html](../../src/main/resources/templates/pets/form.html):**

```html
<label for="species"
       class="mb-2 block text-sm font-bold text-slate-200">
    Especie
</label>

<input id="species"
       type="text"
       th:field="*{species}"
       maxlength="80"
       placeholder="Ej. Perro, gato, conejo">
```

El archivo real agrega también clases de estilo y atributos accesibles que estudiaremos a continuación.

La etiqueta tiene un nombre persistente y se relaciona con el campo mediante `for="species"` e `id="species"`.

El placeholder ofrece un ejemplo, pero **no reemplaza a la etiqueta**: puede desaparecer al escribir y no debe ser la única información que identifique el propósito del control.

### Tipos y atributos con significado

| Atributo | Ejemplo | Qué comunica o facilita |
|---|---|---|
| `type="email"` | Correo | Comportamiento especializado de entrada y teclado móvil. |
| `type="number"` | Edad | Entrada numérica, aunque la validación del servidor sigue siendo necesaria. |
| `min="0"` | Edad | Límite de la interfaz; no sustituye la regla de negocio. |
| `maxlength="100"` | Nombre | Restricción de longitud en el cliente. |
| `autocomplete="name"` | Registro | Ayuda a los mecanismos de autocompletado del navegador. |
| `type="date"` | Fecha | Control de fecha nativo, cuyo aspecto depende del navegador. |

**Advertencia:** los controles HTML son una capa de ayuda; los datos enviados por HTTP se pueden modificar fuera de la interfaz. La validación del servidor y las reglas del dominio siguen siendo necesarias.

---

## 8. Errores: identificar el problema y el campo que debe corregirse

Considera una persona que envía un formulario con nombre vacío y edad negativa. Mostrar solamente «Formulario inválido» no explica cómo resolverlo.

PetMatch vincula cada mensaje con el campo correspondiente utilizando Thymeleaf y atributos ARIA.

**Código real — [pets/form.html](../../src/main/resources/templates/pets/form.html):**

```html
<input id="name"
       type="text"
       th:field="*{name}"
       th:attr="aria-invalid=${#fields.hasErrors('name')},
                aria-describedby=${#fields.hasErrors('name') ? 'name-error' : null}"
       maxlength="100">

<p th:if="${#fields.hasErrors('name')}"
   id="name-error"
   role="alert"
   th:errors="*{name}">
</p>
```

### Descomponer el mecanismo

1. `#fields.hasErrors('name')` consulta si el binding y la validación detectaron errores para ese campo.
2. `aria-invalid` comunica que el valor actual no ha superado la validación.
3. `aria-describedby` enlaza el control con un elemento que contiene información complementaria.
4. `id="name-error"` identifica el mensaje de ese campo.
5. `th:errors="*{name}"` muestra el texto producido por el sistema de validación.
6. `th:if` evita renderizar ese mensaje cuando no existe error.
7. `role="alert"` permite que ciertas tecnologías de apoyo reconozcan el contenido como una alerta.

### ¿Por qué conviene asociar el error?

Porque una persona que recorre el formulario campo por campo necesita saber **cuál** control requiere atención y **por qué**.

El color rojo puede reforzar la indicación visual, pero **no es el mensaje completo**. Debe existir texto comprensible.

### La validación ocurre en varias capas

```text
Formulario HTML
      ↓
Spring MVC + BindingResult
      ↓
Mensaje asociado al campo
      ↓
Usuario corrige y reenvía
```

Los capítulos [21 — Validación](../03-web-mvc/21-validacion.md) y [20 — Formularios y Form DTO](../03-web-mvc/20-formularios-y-form-dto.md) explican cómo se forman esos errores desde el servidor.

### Una distinción importante

`aria-invalid`, `aria-describedby` y `role="alert"` son recursos técnicos. Su presencia **no garantiza por sí sola** un anuncio perfecto en todos los lectores de pantalla ni una experiencia comprensible para todas las personas.

Por ejemplo, también debe revisarse si el foco y el resumen de errores permiten localizar rápidamente el primer problema. Eso requiere pruebas adicionales.

---

## 9. Mensajes de éxito, error y estado del sistema

Una persona necesita saber qué ocurrió después de realizar una acción.

PetMatch reutiliza un fragmento de alertas.

**Código real — [fragments/alerts.html](../../src/main/resources/templates/fragments/alerts.html):**

```html
<div role="status"
     th:if="${successMessage}"
     th:text="${successMessage}">
</div>

<div role="alert"
     th:if="${errorMessage}"
     th:text="${errorMessage}">
</div>
```

El código real añade estilos visuales para cada variante.

### ¿Qué diferencia hay?

- `role="status"` representa información de estado que normalmente no requiere interrumpir urgentemente la interacción.
- `role="alert"` representa información que puede necesitar una comunicación más inmediata.

Los mensajes se generan en el servidor y se insertan en la vista. La efectividad del anuncio depende del momento de inserción y del lector de pantalla: **no debes asumir que una página recargada anuncia automáticamente todo el contenido esperado solo por contener esos roles**.

### Ejemplo de una operación administrativa

Después de cambiar un rol, el Controller muestra un mensaje de éxito que explica que la cuenta afectada debe iniciar sesión nuevamente. Si se intenta degradar al último administrador, presenta un error de negocio.

Ese contenido no solo confirma una respuesta HTTP: explica la consecuencia de la operación y qué espera el sistema a continuación.

**🔁 Llévalo a tu proyecto:** identifica una acción que actualmente muestre únicamente «Guardado». ¿Qué información adicional necesita el usuario para confiar en el resultado?

---

## 10. Tablas comprensibles y resultados vacíos

Una tabla administrativa puede ser extensa. Para leerla correctamente es importante distinguir datos, encabezados y acciones.

**Código real — [admin/users.html](../../src/main/resources/templates/admin/users.html):**

```html
<table class="pm-table">
    <caption class="pm-sr-only">
        Usuarios y permisos persistidos
    </caption>
    <thead>
        <tr>
            <th scope="col">Nombre</th>
            <th scope="col">Correo</th>
            <th scope="col">Rol / estado</th>
            <th scope="col">Cambiar rol</th>
        </tr>
    </thead>
</table>
```

El `caption` comunica la finalidad de la tabla; la clase `.pm-sr-only` lo oculta visualmente sin eliminarlo del árbol accesible. `scope="col"` vincula semánticamente los encabezados a sus columnas.

### Formularios repetidos dentro de tablas

Cuando hay un selector «Rol» por cada usuario, el control debe distinguir **a qué persona pertenece**. PetMatch utiliza una etiqueta contextual:

```html
<label class="pm-sr-only"
       th:for="${'role-' + u.id}"
       th:text="${'Rol de ' + u.name}">
</label>
```

El botón también utiliza una etiqueta accesible que incluye el nombre del usuario objetivo. Esto evita tener una lista de veinte controles indistinguibles llamados simplemente «Guardar».

### Estados vacíos

La vista de reportes contiene un estado vacío con una explicación:

```html
<div class="pm-empty" th:if="${report.rows.empty}">
    <p class="pm-empty__title">
        No hay solicitudes con estos filtros
    </p>
    <p>
        Prueba con otras fechas o elimina los filtros
        para consultar todas las solicitudes.
    </p>
</div>
```

Cero resultados **no es una falla del sistema**. El mensaje distingue una búsqueda válida sin coincidencias de un error de validación o conexión.

**⚠️ Error frecuente:** mostrar una tabla sin filas y sin explicación. La persona no sabe si el sistema está cargando, falló o realmente no encontró resultados.

---

## 11. Diseño adaptable: mantener la tarea posible en una pantalla pequeña

El diseño adaptable no consiste solamente en «hacer que todo sea más pequeño». Debe preservar legibilidad, orden de lectura y controles utilizables.

PetMatch combina utilidades de Tailwind con estilos propios en [application.css](../../src/main/resources/static/css/application.css).

### Filtros administrativos

**Código real — fragmentos de CSS:**

```css
.pm-filter-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1.1rem;
}

@media (max-width: 800px) {
    .pm-filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 540px) {
    .pm-filter-grid {
        grid-template-columns: 1fr;
    }
}
```

A medida que el espacio disponible disminuye, los filtros pasan de tres a dos y finalmente a una columna.

### Tablas administrativas

**Código real:**

```css
.pm-table-wrap {
    overflow-x: auto;
}

.pm-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 700px;
}
```

La tabla conserva un ancho mínimo y su contenedor permite desplazamiento horizontal.

Esta decisión evita comprimir todas las columnas hasta volverlas ilegibles. **Pero también introduce una tarea de evaluación**: comprobar que las personas descubren y pueden utilizar el desplazamiento horizontal, especialmente con teclado, zoom o dispositivos táctiles.

### Navegación adaptable

El menú común usa Flexbox con `flex-wrap`; a determinados anchos, la sección administrativa pasa a una fila separada.

La experiencia móvil debe observarse en el navegador real. Que exista una media query **no equivale a demostrar ausencia de solapamientos, desbordamientos o problemas de lectura**.

### Prueba manual recomendada

Comprueba al menos:

- pantalla estrecha, por ejemplo 390 píxeles de ancho;
- anchura intermedia;
- escritorio;
- ampliación de texto y zoom;
- tablas con nombres y títulos largos;
- navegación con el teclado y foco que permanezca visible.

Registra los resultados observados. No declares «responsive verificado» solo porque el archivo CSS contenga reglas `@media`.

---

## 12. Modo claro y oscuro: una preferencia, no un requisito universal

PetMatch **sí tiene actualmente** un selector de tema claro/oscuro. Está implementado mediante [theme.js](../../src/main/resources/static/js/theme.js) y [theme.css](../../src/main/resources/static/css/theme.css).

El objetivo es permitir una presentación alternativa, conservar la preferencia y mantener la interfaz utilizable en ambos modos.

### 12.1 Preferencia inicial del sistema

**Código real — `theme.js`:**

```javascript
const storageKey = "petmatch-theme";
const root = document.documentElement;
const systemPreference = window.matchMedia(
    "(prefers-color-scheme: light)"
);
```

El script utiliza `matchMedia` para consultar la preferencia actual del sistema.

### 12.2 Preferencia explícita del usuario

**Código real — fragmento:**

```javascript
function selectedTheme() {
    return savedPreference()
        ?? (systemPreference.matches ? "light" : "dark");
}
```

La preferencia guardada tiene prioridad; si no existe una selección válida, se utiliza el sistema.

El script maneja mediante `try/catch` el posible bloqueo de `localStorage`. Si el almacenamiento falla, **la selección se aplica en la página actual**, aunque no se garantiza que persista después de recargar.

### 12.3 Cambiar la apariencia sin recargar

**Código real — fragmento:**

```javascript
function setTheme(theme) {
    root.dataset.theme = theme;
    root.style.colorScheme = theme;
}
```

La función real también actualiza el texto, título y atributos del botón.

`theme.css` usa selectores como:

```css
html[data-theme="light"] .pm-panel,
html[data-theme="light"] .pm-nav {
    background-color: #ffffff !important;
}
```

El control se crea dinámicamente cuando el DOM está disponible y utiliza un elemento `button`, de modo que recibe comportamiento de teclado nativo.

### 12.4 Nombre accesible y estado del botón

El script actualiza el nombre de la acción:

```javascript
toggle.setAttribute("aria-label", "Cambiar a modo " + target);
toggle.setAttribute("aria-pressed", String(theme === "light"));
```

El texto visible comunica el **modo al que cambiaría** al activar el botón. `aria-pressed` expresa un estado binario asociado al modo claro.

**Criterio de revisión:** verifica con lectores de pantalla si el nombre de acción y el estado son comprensibles juntos. No añadas ARIA solo porque una propiedad existe: su semántica debe ser clara.

### 12.5 ¿Un tema claro/oscuro demuestra accesibilidad?

No. Un tema alternativo puede mejorar preferencias individuales, pero **no es una exigencia universal de WCAG** ni garantiza contraste correcto.

Todavía se deben comprobar:

- texto normal y texto grande en ambas paletas;
- bordes y estados de controles;
- foco visible;
- errores, éxito y enlaces;
- gráficos, iconos y cualquier información transmitida mediante color;
- zoom, móvil y preferencias de reducción de movimiento.

**Limitación real:** la implementación de tema utiliza overrides específicos sobre estilos del proyecto y utilidades de Tailwind. Debe evaluarse visualmente en todas las vistas, no solo en las páginas administrativas.

---

## 13. Estilos compartidos y dependencia de Tailwind mediante CDN

El fragmento [head.html](../../src/main/resources/templates/fragments/head.html) carga:

**Código real:**

```html
<script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>

<link rel="stylesheet" th:href="@{/css/accessibility.css}">
<link rel="stylesheet" th:href="@{/css/application.css}">
<link rel="stylesheet" th:href="@{/css/theme.css}">

<script th:src="@{/js/theme.js}" defer></script>
<script th:src="@{/js/navigation.js}" defer></script>
```

Aquí hay **dos mecanismos de estilos**:

1. Las plantillas usan clases de utilidad que el navegador obtiene a través del script de Tailwind Play CDN.
2. Algunos componentes comunes tienen estilos locales en `application.css`, `accessibility.css` y `theme.css`.

### ¿Qué significa esto para el usuario?

Si la conexión al CDN falla, la aplicación todavía dispone de ciertos estilos locales, en particular para navegación y vistas administrativas. **No se puede prometer que todas las pantallas mantengan su diseño completo**, porque muchas utilidades de otras plantillas dependen del script externo.

El Play CDN es cómodo para demostraciones y desarrollo exploratorio, pero no sustituye una estrategia de compilación y publicación de CSS local para un despliegue productivo robusto.

Esta es una **limitación de desarrollo pendiente**, no una mejora que el capítulo deba presentar como implementada.

### Alternativas que podrías evaluar

- Compilar CSS durante el proceso de construcción.
- Publicar un archivo de estilos estático versionado con la aplicación.
- Mantener reglas propias de componentes cuando su tamaño y reutilización lo justifican.

La elección debe considerar reproducibilidad del build, disponibilidad de red, mantenimiento y necesidades del equipo. No hace falta añadir un framework frontend SPA para resolver estos puntos.

---

## 14. Accesibilidad y WCAG 2.2: utilizar criterios, no etiquetas de certificación

Las **Web Content Accessibility Guidelines (WCAG) 2.2**, publicadas por W3C, ofrecen criterios de éxito para evaluar accesibilidad web.

Su estructura se organiza en cuatro principios:

| Principio | Pregunta |
|---|---|
| **Perceptible** | ¿La información se puede percibir? |
| **Operable** | ¿Se pueden utilizar los controles y la navegación? |
| **Comprensible** | ¿La información y la interacción se entienden? |
| **Robusto** | ¿El contenido puede interpretarse adecuadamente por agentes de usuario y tecnologías de apoyo? |

### Relacionar criterios con decisiones concretas

| Criterio WCAG 2.2 | Tema que permite estudiar | Ejemplo en PetMatch |
|---|---|---|
| 1.3.1 — Información y relaciones | Semántica estructural | Etiquetas y encabezados de tabla |
| 1.4.3 — Contraste mínimo | Legibilidad del texto | Colores de texto en temas claro y oscuro |
| 1.4.11 — Contraste no textual | Estados y límites de controles | Bordes, botones e indicadores |
| 2.1.1 — Teclado | Operabilidad sin ratón | Formularios y selector de tema |
| 2.4.1 — Evitar bloques | Saltar navegación repetida | Enlace «Saltar al contenido» |
| 2.4.7 — Foco visible | Reconocer el control activo | Regla compartida `:focus-visible` |
| 2.4.11 — Foco no oculto (mínimo) | Foco perceptible | Revisión con botones flotantes y scroll |
| 3.3.1 — Identificación de errores | Comprender fallos de entrada | Mensajes por campo |
| 3.3.2 — Etiquetas o instrucciones | Entender qué se solicita | `<label>`, ayuda y campos contextualizados |
| 4.1.3 — Mensajes de estado | Comunicar resultados sin trasladar foco | `role="status"` en alertas |

Las relaciones anteriores son **puntos de análisis**, no una afirmación de conformidad.

### Contraste: no basta con elegir colores bonitos

Para el criterio de contraste mínimo de texto, WCAG AA establece como regla general una relación de al menos **4,5:1 para texto normal y 3:1 para texto grande**, con condiciones y excepciones definidas por el estándar. Los controles e indicadores gráficos significativos tienen otras reglas aplicables.

No hemos medido aquí todas las combinaciones de primer plano, fondo, transparencia, foco, hover, mensajes y temas. Por eso sería incorrecto afirmar que la aplicación cumple integralmente WCAG 2.2 AA.

### Lo que una auditoría responsable debe decir

Una evaluación debe registrar:

- criterio concreto;
- página y estado de interfaz;
- navegador, viewport y configuración;
- pasos de reproducción;
- resultado observado;
- evidencia;
- alcance y limitación;
- conclusión proporcional a lo verificado.

**⚠️ Error frecuente:** concluir «sitio certificado AA» porque existen atributos ARIA, una herramienta automatizada no encontró errores o los colores parecen contrastar.

**Referencia oficial:** [WCAG 2.2 de W3C](https://www.w3.org/TR/WCAG22/).

---

## 15. Comprobaciones automatizadas: qué prueban y qué no prueban

PetMatch incorpora una comprobación estática y pruebas HTTP. También incluye un script opcional de navegador.

### 15.1 Comprobación estática de errores de campo

El archivo [tools/check-accessibility.py](../../tools/check-accessibility.py) revisa estas plantillas:

```text
auth/register.html
pets/form.html
support-requests/form.html
support-applications/form.html
```

Busca campos declarados con `th:errors` y comprueba, mediante coincidencias de texto, la presencia de referencias e IDs de error relacionados.

**¿Qué puede demostrar?**

Que en esos archivos existe el patrón textual esperado para vincular mensajes a controles.

**¿Qué no puede demostrar?**

- que el HTML final tenga relaciones correctas en cada estado posible;
- que todos los campos del sitio se revisaron;
- que el teclado funcione;
- que los anuncios de lector de pantalla sean adecuados;
- que los contrastes cumplan los umbrales de WCAG;
- que un usuario comprenda el formulario.

El script se presenta a sí mismo como una **regresión estática, no una auditoría WCAG**.

### 15.2 Integración HTTP con MockMvc

En [WebRegressionTests.java](../../src/test/java/com/petmatch/community/integration/WebRegressionTests.java), la prueba `sessionLoginAndCsrfRemainEnabled` envía datos inválidos al registro de una mascota.

El test comprueba errores del modelo y busca que el HTML contenga:

```html
aria-invalid="true"
```

Esto verifica parte de la respuesta generada por Spring MVC y Thymeleaf. No realiza una interacción humana ni prueba el orden completo de tabulación.

En [AdminIntegrationTests.java](../../src/test/java/com/petmatch/community/integration/AdminIntegrationTests.java), la prueba `adminScreensRenderLocalStylesAndAccessibleNavigation` comprueba, entre otros elementos:

- presencia de clases de navegación y tabla;
- disponibilidad de archivos CSS locales;
- disponibilidad de `theme.js` y su uso de `localStorage`;
- presencia de `aria-pressed` en el script;
- disponibilidad de `navigation.js` con `aria-current`.

Que un archivo JavaScript contenga la cadena `aria-current` **no significa que el navegador haya seleccionado correctamente todos los enlaces**. Esa comprobación requiere ejecución en el navegador.

### 15.3 Browser smoke: un recorrido automatizado opcional

El script [tools/browser-smoke.py](../../tools/browser-smoke.py) utiliza Playwright y Chromium, con una base de datos **sintética y aislada**.

Su recorrido incluye:

- registro e inicio de sesión de cuentas de prueba;
- mascota y solicitud;
- postulación y aceptación;
- cambio de roles y reportes;
- alternancia y persistencia del tema;
- descarga PDF y Excel;
- estado de navegación;
- enlace para saltar al contenido;
- captura de la pantalla administrativa en un viewport estrecho.

El script está acoplado a un entorno local específico: aplicación en `127.0.0.1:8081`, navegador Chromium disponible y un contenedor de MySQL de pruebas configurado con el esquema previsto.

**Importante:** en la configuración vigente de CI se comprueba la **sintaxis Python** del script con `py_compile`; **no se ejecuta el recorrido Playwright completo** como parte de esa etapa.

La presencia del script no demuestra que el recorrido haya pasado en el commit actual, ni que exista una evaluación humana.

### 15.4 La CI y sus límites

La configuración [ci.yml](../../.github/workflows/ci.yml) ejecuta Maven, comprueba asociaciones estáticas de errores y revisa la sintaxis del smoke script.

Es evidencia técnica útil, pero no sustituye:

- prueba manual de teclado y zoom;
- lector de pantalla;
- contrastes medidos en todos los estados;
- evaluación de personas representativas;
- validación de la facilidad de comprensión.

**🧠 Idea mental**

```text
Prueba estática
    → detecta patrones del código

Prueba HTTP / integración
    → comprueba respuestas y reglas del servidor

Prueba real de navegador
    → comprueba interacciones en un motor web

Evaluación humana
    → observa si la persona puede completar la tarea
```

Estas evidencias se **complementan**; ninguna convierte automáticamente a las otras en innecesarias.

---

## 16. Diseñar una evaluación humana reproducible

Una evaluación de usabilidad no requiere necesariamente un laboratorio complejo, pero sí debe tener una tarea clara, observaciones y un registro honesto.

### Caso A — Registro, mascota y solicitud

**Objetivo de la persona:** publicar una solicitud de ayuda para su mascota.

**Escenario:** dispone de una cuenta nueva y todavía no registró una mascota.

**Tareas:**

1. Registrarse e iniciar sesión.
2. Encontrar «Mis mascotas».
3. Enviar un formulario con un valor inválido.
4. Identificar el error y corregirlo.
5. Crear la mascota.
6. Acceder a «Nueva solicitud».
7. Completar la solicitud y confirmar su publicación.

**Observa:** dónde duda, si entiende los mensajes, si necesita orientación, si abandona una pantalla y si percibe que terminó el proceso.

### Caso B — Solicitud y postulación

**Objetivo de dos personas:** una solicita apoyo y la otra se postula.

**Tareas:**

1. La primera publica una solicitud.
2. La segunda la encuentra y se postula.
3. La primera acepta una postulación.
4. Ambas consultan el nuevo estado.

**Observa:** si cada persona sabe qué puede hacer y qué significa el resultado. La etiqueta del estado debe poder entenderse **sin depender únicamente del color**.

### Caso C — Administración y reportes

**Objetivo:** un administrador necesita consultar solicitudes filtradas y descargar los resultados.

**Tareas:**

1. Localizar el área administrativa.
2. Abrir reportes y aplicar dos filtros.
3. Comprender el total y el resumen.
4. Navegar a otra página sin perder filtros.
5. Cambiar el tema claro/oscuro y recargar.
6. Descargar PDF o Excel.
7. Repetir una parte del recorrido con teclado y pantalla estrecha.

**Observa:** orientación, legibilidad, claridad de filtros, foco, comprensión de errores y facilidad de acceso a la descarga.

### Registro básico de observaciones

| Campo | Qué registrar |
|---|---|
| Fecha y versión | Momento y commit probado |
| Contexto | Equipo, navegador y viewport |
| Persona evaluadora | Perfil relevante, sin recopilar datos personales innecesarios |
| Tarea | Resultado que se le solicitó alcanzar |
| Resultado | Completada, no completada o parcialmente completada |
| Evidencia | Pasos, dificultad observada, captura autorizada o nota |
| Severidad | Efecto sobre la posibilidad de completar la tarea |
| Acción | Mejora propuesta y forma de verificarla |

No inventes participantes ni tiempos de ejecución. Si todavía no se ha hecho la sesión, registra **pendiente de evaluación**.

---

## 17. Cómo transformar un hallazgo en un criterio verificable

Una observación vaga como «el formulario confunde» no basta para orientar un cambio.

### Ejemplo conceptual: error difícil de localizar

**Observación:** la persona envía un formulario y no identifica qué campo requiere corrección.

**Hipótesis:** el mensaje no está suficientemente relacionado con el control o no queda visible tras el envío.

**Criterio de aceptación propuesto:**

> Al enviar un formulario con una edad negativa, el usuario puede identificar el campo Edad y leer una explicación comprensible de qué debe corregir.

### Separar comprobaciones

| Comprobación | Qué evidencia ofrece |
|---|---|
| Test de Bean Validation | El valor inválido se rechaza |
| Test MockMvc | La vista recibe errores y muestra atributos esperados |
| Revisión DOM | El ID del error coincide con `aria-describedby` |
| Recorrido con teclado | La persona alcanza el control y percibe su estado |
| Prueba con lector de pantalla | Se comprueba cómo se anuncia el campo y el error |
| Observación humana | La persona comprende qué debe hacer |

**No confundas cumplimiento de una condición técnica con éxito completo de la tarea.**

### Priorizar por el efecto

No todas las mejoras tienen la misma urgencia.

- Un botón que no puede activarse por teclado puede impedir por completo una tarea.
- Un mensaje ambiguo puede provocar errores repetidos.
- Un encabezado con jerarquía imperfecta puede dificultar la orientación.
- Un detalle puramente estético puede tener menor impacto funcional.

La priorización debe considerar impacto real, frecuencia, gravedad y esfuerzo. No agregues una puntuación artificial si no tienes criterios para interpretarla.

---

## 18. Qué conserva PetMatch y qué necesita verificación adicional

Es importante terminar el análisis con un inventario proporcional a la evidencia.

| Capacidad o criterio | Situación observada en el código | Qué falta comprobar |
|---|---|---|
| Navegación compartida | Fragmento Thymeleaf reutilizado | Comprensión del menú con usuarios |
| Sección actual | Script asigna `aria-current` y clase activa | Rutas anidadas y lectores de pantalla |
| Foco visible | Regla compartida `:focus-visible` | Contraste, visibilidad y no ocultamiento |
| Salto al contenido | Enlace presente en plantillas | Comportamiento en navegadores y teclado |
| Errores por campo | `aria-invalid`, `aria-describedby`, mensajes | Foco, anuncios y comprensión |
| Mensajes de estado | Fragmento con `role="status"` y `role="alert"` | Anuncios reales con tecnologías de apoyo |
| Tablas administrativas | `caption`, encabezados y selector por usuario | Navegación con zoom y scroll horizontal |
| Filtros adaptables | Grid con tres, dos y una columna | Pantallas y combinaciones de contenido |
| Tema claro/oscuro | JavaScript y CSS con persistencia local | Contraste y apariencia en todas las vistas |
| CSS en producción | Tailwind mediante Play CDN y CSS parcial local | Build de CSS local reproducible |
| Pruebas automatizadas | HTTP, comprobación estática y script opcional | Ejecución integral del navegador y pruebas humanas |
| Conformidad WCAG 2.2 AA | No certificada | Evaluación de criterios aplicables |

Un registro técnico responsable puede afirmar que **existen mecanismos implementados** y también señalar que **no hay evidencia suficiente para declarar cumplimiento integral**.

Si se descubre un fallo, no debe esconderse detrás de la frase «las pruebas pasaron»: hay que transformarlo en un caso reproducible y corregirlo.

---

## 19. ⚠️ Errores frecuentes y decisiones alternativas

### Error A — Utilizar únicamente color para indicar un estado

**Problema:** la persona no sabe si una solicitud fue aceptada si el único indicador es verde.

**Mejor decisión:** combinar texto explícito, estructura y color complementario.

### Error B — Reemplazar botones nativos por elementos genéricos

**Problema:** un `<div>` con evento de clic no recibe automáticamente semántica y comportamiento de teclado.

**Mejor decisión:** utilizar `<button>` o `<a>` según sea una acción o una navegación.

### Error C — Suponer que el placeholder equivale a un label

**Problema:** el texto de ejemplo desaparece y no representa una etiqueta persistente.

**Mejor decisión:** proporcionar una etiqueta asociada y una ayuda opcional.

### Error D — Presentar `aria-invalid` como validación del servidor

**Problema:** ARIA comunica el estado, pero no decide si el dato es correcto.

**Mejor decisión:** mantener Bean Validation y las reglas del dominio, y presentar sus resultados de forma comprensible.

### Error E — Confundir responsive con accesibilidad

**Problema:** una tabla puede caber mediante scroll y seguir siendo difícil de operar con zoom o teclado.

**Mejor decisión:** comprobar el comportamiento real y el recorrido completo.

### Error F — Declarar WCAG AA después de ejecutar un script

**Problema:** una comprobación estática revisa un subconjunto de reglas y no observa a las personas.

**Mejor decisión:** relacionar cada evidencia con el criterio evaluado y declarar pendientes.

### Error G — Forzar un framework de frontend

**Problema:** pensar que para mejorar navegación, temas o mensajes es obligatorio sustituir Thymeleaf por una SPA.

**Mejor decisión:** corregir primero las plantillas, estilos y scripts actuales. Cambiar de arquitectura requiere una necesidad adicional.

### Error H — Dar por validado el modo claro sin revisar pantallas

**Problema:** una hoja de overrides puede cubrir las pantallas administrativas y dejar combinaciones inconsistentes en otras vistas.

**Mejor decisión:** probar ambos temas en cada recorrido crítico con distintos tamaños y estados de error.

---

## 20. 🔁 Llévalo a tu proyecto

No copies las clases CSS de PetMatch ni conviertas todos tus formularios en páginas idénticas. Trabaja sobre una necesidad real de tu aplicación.

### Paso 1 — Selecciona un recorrido

Elige una tarea importante: registrar un préstamo, hacer una reserva, actualizar un producto o consultar una solicitud.

Escribe cuál es su resultado esperado y qué acciones debe realizar la persona, desde la primera pantalla hasta la confirmación.

### Paso 2 — Distingue las dimensiones

Para el mismo recorrido, define:

- un criterio de **usabilidad**;
- un criterio de **navegación**;
- un criterio de **accesibilidad**;
- una condición de **diseño adaptable** si corresponde.

Evita escribir «que sea fácil de usar». Debes indicar qué observarás para comprobarlo.

### Paso 3 — Examina el HTML

Identifica el `main`, los encabezados, enlaces, botones y etiquetas de formulario. Comprueba que cada control tiene un propósito reconocible.

### Paso 4 — Reproduce un error

Envía datos inválidos. Comprueba que el sistema indica el problema y relaciona el mensaje con el campo correcto. Diferencia lo que garantiza el backend de lo que comunica la vista.

### Paso 5 — Prueba el teclado

Sin utilizar ratón, recorre la pantalla con Tab y Shift+Tab. Registra el orden, la visibilidad del foco y cualquier control inaccesible.

### Paso 6 — Revisa móvil y zoom

Utiliza un viewport estrecho, amplía el texto y comprueba los componentes con contenido largo. Anota si aparecen solapamientos o desplazamientos inesperados.

### Paso 7 — Compara evidencia técnica y humana

Crea una prueba automatizada cuando exista un resultado técnico repetible. Después prepara una sesión con una persona que pueda representar el uso previsto.

Si aún no realizaste la sesión, no inventes sus resultados.

### Paso 8 — Prioriza una mejora

Elige el hallazgo más relevante, identifica una causa probable, implementa una corrección proporcionada y repite la misma verificación.

**Evidencia sugerida:** recorrido, criterios, resultados observados, una mejora justificada y su comprobación. No necesitas elaborar un PDF ni incorporar herramientas que tu aplicación no requiera.

---

## 21. 🧪 Comprueba que entendiste

Responde sin consultar el código:

1. ¿Qué diferencia existe entre usabilidad y accesibilidad?
2. ¿Por qué una vista técnicamente correcta puede producir una mala experiencia?
3. ¿Qué función tiene `<main>` en un documento?
4. ¿Por qué un `<button>` suele ser preferible a un `<div onclick>` para ejecutar una acción?
5. ¿Qué aporta reutilizar la navegación con Thymeleaf?
6. ¿Qué comunica `aria-current="page"` y qué limitación tiene inferirlo solamente desde la URL?
7. ¿Para qué sirve el enlace «Saltar al contenido»?
8. ¿Por qué `:focus-visible` es importante para teclado?
9. ¿Por qué una etiqueta `<label>` no debería reemplazarse por un placeholder?
10. ¿Cómo se relacionan `aria-invalid` y `aria-describedby` con los mensajes del formulario?
11. ¿Por qué un mensaje de error debe explicar el problema y no depender únicamente del color?
12. ¿Qué utilidad tienen `caption` y `scope="col"` en una tabla?
13. ¿Qué diferencia existe entre hacer scroll horizontal y ofrecer una tabla realmente utilizable en móvil?
14. ¿Cómo decide PetMatch el tema inicial cuando no existe una preferencia guardada?
15. ¿Por qué un selector claro/oscuro no demuestra conformidad WCAG AA?
16. ¿Qué problema introduce depender de Tailwind mediante Play CDN?
17. ¿Qué comprueba `check-accessibility.py` y qué no puede comprobar?
18. ¿Por qué `python3 -m py_compile tools/browser-smoke.py` no equivale a ejecutar Playwright?
19. ¿Qué evidencia exigirías antes de afirmar que la navegación con teclado está validada?
20. ¿Cómo convertirías la queja «no entiendo el formulario» en un criterio verificable?

### Diagnóstico de un escenario

Una persona abre el formulario de registro de mascotas, deja el nombre vacío, escribe una edad negativa y presiona «Registrar mascota».

Explica, en orden:

1. qué debe decidir la validación del servidor;
2. qué información debe recibir Thymeleaf;
3. qué elemento identifica cada campo;
4. cómo se relaciona un mensaje con su control;
5. qué debería observar una persona que utiliza teclado;
6. qué comprobarías con MockMvc;
7. qué comprobarías con un navegador real;
8. qué preguntarías durante una prueba de usabilidad.

La respuesta debe distinguir **lo que PetMatch implementa**, **lo que se puede inferir del código** y **lo que todavía necesita evaluación humana**.

---

## ✅ Qué debes recordar

Una interfaz de calidad no depende de una biblioteca visual popular ni de una lista extensa de atributos ARIA.

**La navegación orienta; el HTML semántico expresa la estructura; los controles y el foco permiten operar; los mensajes ayudan a comprender; y la evaluación observa si la persona puede completar su tarea.**

PetMatch ofrece ejemplos útiles de esas responsabilidades con Thymeleaf, CSS y JavaScript sencillo. También muestra por qué es necesario declarar los límites: contar asociaciones de errores, disponer de un modo claro/oscuro o superar pruebas HTTP **no equivale** a una certificación de accesibilidad o a una validación humana.

Aplica estos conceptos al flujo más importante de tu propio proyecto y demuestra sus resultados con evidencia honesta y reproducible.

### Para continuar

- [20 — Formularios y Form DTO](../03-web-mvc/20-formularios-y-form-dto.md).
- [21 — Validación](../03-web-mvc/21-validacion.md).
- [25 — Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md).
- [31 — Pruebas unitarias](../06-calidad-y-recorrido/31-pruebas-unitarias.md).
- [32 — Pruebas de integración](../06-calidad-y-recorrido/32-pruebas-de-integracion.md).
- [40 — Administración de roles, autorización y auditoría](40-roles-autorizacion-y-auditoria.md).
- [41 — Reportes dinámicos con JPA](41-reportes-dinamicos-jpa.md).
- [42 — Exportación de reportes PDF y Excel](42-exportacion-reportes-pdf-excel-strategy.md).

### Referencias oficiales

- [W3C — Web Content Accessibility Guidelines (WCAG) 2.2](https://www.w3.org/TR/WCAG22/).
- [W3C WAI — Introducción a la accesibilidad web](https://www.w3.org/WAI/fundamentals/accessibility-intro/).
- [W3C WAI — Tutorial de formularios](https://www.w3.org/WAI/tutorials/forms/).
- [W3C WAI — Evaluar la accesibilidad web](https://www.w3.org/WAI/test-evaluate/).
- [MDN — `:focus-visible`](https://developer.mozilla.org/en-US/docs/Web/CSS/:focus-visible).
- [MDN — `aria-describedby`](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-describedby).
