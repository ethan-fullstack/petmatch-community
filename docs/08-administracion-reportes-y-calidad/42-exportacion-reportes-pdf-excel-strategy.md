# 42 — Exportación de reportes PDF y Excel: Strategy cuando la variación es real

En el [capítulo 41](41-reportes-dinamicos-jpa.md) construimos un reporte administrativo con filtros, proyecciones, total y agrupación. La aplicación ya puede responder qué solicitudes cumplen una búsqueda. Ahora aparece una necesidad diferente:

> **¿Cómo entregar esos mismos resultados como un documento PDF o como un libro de Excel, sin duplicar las consultas ni llenar el Controller de condiciones para cada formato?**

Un PDF se diseña como páginas de contenido. Un archivo XLSX es un libro de cálculo con hojas, filas, celdas y tipos de datos. Los dos comparten la **información**, pero no el **algoritmo de representación**. Aquí sí existe una razón técnica para aplicar **Strategy**, uno de los patrones de comportamiento del catálogo GoF.

Estudiaremos el código que ya funciona en PetMatch Community. No añadiremos formatos imaginarios para justificar el patrón. Al finalizar podrás seguir una descarga desde el enlace Thymeleaf hasta la respuesta HTTP, explicar las responsabilidades de cada clase, verificar archivos reales, reconocer riesgos de seguridad y trasladar el diseño a otra aplicación.

---

## 1. El problema: visualizar no es lo mismo que exportar

La tabla de reportes muestra **20 solicitudes por página**. La descarga, en cambio, debe incluir todos los registros que cumplen los filtros, **hasta el límite permitido**, no únicamente los 20 visibles.

Imagina que la búsqueda devuelve 26 solicitudes:

| Operación | Resultado esperado |
|---|---|
| Ver primera página | 20 solicitudes |
| Ver segunda página | 6 solicitudes |
| Descargar PDF | 26 solicitudes y resumen |
| Descargar Excel | 26 solicitudes y resumen |
| Cambiar agrupación | Cambia el resumen, no la población |

La descarga no debe reconstruir los filtros desde el HTML ni consultar una población distinta.

**🧠 Idea mental**

```text
Filtros validados
      |
      v
Repository: mismas solicitudes y agrupación
      |
      v
ReportExportService
      |
      +--> PdfReportExporter   --> bytes PDF
      |
      +--> ExcelReportExporter --> bytes XLSX
      |
      v
HTTP: tipo de archivo + nombre + contenido
```

**Antes de continuar:** ¿por qué sería incorrecto exportar directamente las filas de `report.rows.getContent()` obtenidas para la primera página?

---

## 2. ¿Qué se comparte y qué cambia?

El diseño empieza separando responsabilidades:

| Responsabilidad | ¿Es común? | Clase responsable |
|---|---|---|
| Interpretar y validar filtros | Sí | `SupportRequestReportFilter` |
| Obtener registros | Sí | `SupportRequestReportRepository` |
| Obtener resumen agrupado | Sí | `SupportRequestReportRepository` |
| Aplicar límite de exportación | Sí | `ReportExportService` |
| Escoger el formato | Sí | `ReportExportService` |
| Dibujar páginas y texto PDF | No | `PdfReportExporter` |
| Construir hojas, celdas y estilos | No | `ExcelReportExporter` |
| Responder HTTP y proteger la ruta | Sí | `AdminReportExportController` y Spring Security |

Si las clases PDF y Excel consultaran la base de datos por separado, podrían introducir filtros distintos. Si el Controller construyera ambos archivos, mezclaría HTTP, consultas y formatos.

La separación del proyecto no es una división por obligación académica: **hay dos implementaciones con algoritmos y dependencias diferentes**.

---

## 3. Del formulario a la URL de descarga

La vista [admin/reports.html](../../src/main/resources/templates/admin/reports.html) ofrece enlaces a ambos formatos. Los parámetros de búsqueda se conservan al construir la URL.

**Código real — enlace PDF:**

```html
<a th:href="@{/admin/reports/export/pdf(
        status=${filter.status},
        supportType=${filter.supportType},
        fromDate=${filter.fromDate},
        toDate=${filter.toDate},
        grouping=${filter.grouping})}">
    Descargar PDF
</a>
```

La plantilla tiene un enlace equivalente para `/admin/reports/export/xlsx`.

Una URL ilustrativa sería:

```text
/admin/reports/export/pdf?status=OPEN&supportType=WALK&grouping=STATUS
```

El navegador realiza una petición `GET`. **Que la ruta sea GET no significa que sea pública**: sigue bajo `/admin/**` y la autorización administrativa se aplica también a las descargas.

La exportación utiliza los filtros del formulario, no un parámetro `page`, porque la finalidad es descargar toda la población filtrada dentro del límite.

---

## 4. El Controller: traducir HTTP, no construir archivos

El endpoint real está en [AdminReportExportController.java](../../src/main/java/com/petmatch/community/controller/AdminReportExportController.java).

**Código real — estructura del método:**

```java
@Controller
@RequestMapping("/admin/reports/export")
public class AdminReportExportController {
    private final ReportExportService service;

    public AdminReportExportController(ReportExportService service) {
        this.service = service;
    }

    @GetMapping("/{format}")
    public ResponseEntity<byte[]> export(
            @PathVariable String format,
            @Valid @ModelAttribute("filter") SupportRequestReportFilter filter,
            BindingResult errors) {
        // Validate HTTP input, delegate, and build the response.
    }
}
```

La versión completa del método:

1. devuelve **400 Bad Request** cuando `BindingResult` contiene errores;
2. interpreta `format` mediante `ReportExportFormat.parse(format)`;
3. devuelve **404 Not Found** si el formato no está soportado;
4. delega en `ReportExportService.export(filter, parsed)`;
5. devuelve **200 OK** con tipo MIME, nombre y bytes cuando la exportación tiene éxito;
6. transforma el exceso de filas en **413 Payload Too Large**.

**Código real — respuesta:**

```java
return ResponseEntity.ok()
    .contentType(file.mediaType())
    .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + file.filename() + "\"")
    .cacheControl(CacheControl.noStore())
    .body(file.content());
```

### ¿Qué representa cada elemento?

- `ResponseEntity<byte[]>`: la respuesta contiene bytes, no una plantilla Thymeleaf.
- `Content-Type`: indica el formato del archivo.
- `Content-Disposition: attachment`: solicita al navegador tratarlo como descarga con nombre sugerido.
- `Cache-Control: no-store`: indica a cachés compatibles que no almacenen la respuesta.
- `body(file.content())`: entrega el contenido binario.

No confundas `no-store` con cifrado, ni `Content-Disposition` con una autorización. Son responsabilidades distintas.

---

## 5. Un enum restringe los formatos permitidos

PetMatch no acepta nombres arbitrarios de clases, extensiones o bibliotecas desde la URL.

**Código real — [ReportExportFormat.java](../../src/main/java/com/petmatch/community/dto/report/ReportExportFormat.java):**

```java
public enum ReportExportFormat {
    PDF("pdf"), XLSX("xlsx");

    private final String extension;

    ReportExportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static ReportExportFormat parse(String value) {
        for (var format : values())
            if (format.extension.equalsIgnoreCase(value))
                return format;
        throw new IllegalArgumentException("Unsupported export format");
    }
}
```

`/pdf` y `/xlsx` son válidos; `/html` no está implementado. El enum delimita el contrato, y el Controller convierte el error de formato en 404.

**Pregunta:** ¿por qué es más seguro y mantenible interpretar un conjunto cerrado de formatos que utilizar directamente el texto de la URL para decidir qué clase instanciar?

---

## 6. El contrato común: la interfaz Strategy

**Código real — [ReportExporter.java](../../src/main/java/com/petmatch/community/service/report/ReportExporter.java):**

```java
public interface ReportExporter {
    ReportExportFormat format();

    MediaType mediaType();

    byte[] export(
            SupportRequestReportFilter filter,
            List<ReportRow> rows,
            List<ReportSummary> summary) throws IOException;
}
```

Este es el contrato común de las estrategias.

- `format()` identifica la implementación.
- `mediaType()` indica cómo comunicar el archivo al navegador.
- `export(...)` transforma **datos ya consultados** en bytes.

La interfaz no depende de `PDDocument`, `XSSFWorkbook` ni de detalles de HTTP Controller. Los implementadores deciden cómo producir el archivo.

### El patrón GoF en términos sencillos

**Strategy** define una familia de algoritmos, encapsula cada uno detrás de un contrato común y permite elegir cuál ejecutar sin que el cliente conozca los detalles internos.

En este caso:

| Elemento del patrón | Implementación en PetMatch |
|---|---|
| Strategy | `ReportExporter` |
| Concrete Strategy A | `PdfReportExporter` |
| Concrete Strategy B | `ExcelReportExporter` |
| Contexto/orquestador | `ReportExportService` |
| Selección | `ReportExportFormat` y `EnumMap` |

No necesitas herencia entre las clases PDF y Excel: **ambas implementan la misma interfaz**.

---

## 7. PDF y Excel son algoritmos genuinamente diferentes

### PDF: páginas y coordenadas

La implementación [PdfReportExporter.java](../../src/main/java/com/petmatch/community/service/report/PdfReportExporter.java) utiliza **Apache PDFBox 3.0.5**.

**Código real — comienzo del método:**

```java
@Override
public byte[] export(
        SupportRequestReportFilter filter,
        List<ReportRow> rows,
        List<ReportSummary> summary) throws IOException {

    try (var document = new PDDocument();
         var output = new ByteArrayOutputStream()) {
        // Create pages, position text, and write the summary.
        document.save(output);
        return output.toByteArray();
    }
}
```

La clase real:

- crea páginas A4 en orientación horizontal;
- escribe título, criterios, total y número de página;
- dispone seis columnas usando posiciones y anchos;
- agrega nuevas páginas al agotarse el espacio;
- añade el resumen agrupado;
- cierra el documento y sus recursos.

PDFBox trabaja con **páginas, coordenadas y flujos de dibujo**. No existe allí el concepto de «celda de Excel».

**Límite importante:** el exportador usa fuentes PDF estándar y reemplaza caracteres que esas fuentes no admiten; también recorta texto que no cabe en el ancho asignado. Por tanto, **no garantiza fidelidad Unicode ni visualización íntegra de títulos extensos**. Es un punto de mejora si los requisitos exigen documentos multilingües o texto sin truncamiento.

### Excel: hojas y tipos de celda

La implementación [ExcelReportExporter.java](../../src/main/java/com/petmatch/community/service/report/ExcelReportExporter.java) utiliza **Apache POI 5.4.1**.

**Código real — estructura esencial:**

```java
try (var workbook = new XSSFWorkbook();
     var output = new ByteArrayOutputStream()) {

    var sheet = workbook.createSheet("Requests");
    var totals = workbook.createSheet("Summary");

    // Populate rows, cell values, styles, and summary.
    workbook.write(output);
    return output.toByteArray();
}
```

El libro contiene dos hojas:

| Hoja | Contenido |
|---|---|
| `Requests` | Cabeceras y una fila por solicitud exportada |
| `Summary` | Agrupación, total filtrado y cantidades por grupo |

El exportador aplica estilos a las cabeceras, inmoviliza la primera fila de solicitudes y activa un autofiltro cuando hay registros.

**Detalle técnico importante:** las fechas se escriben como valores de fecha nativos de Excel y reciben un formato de visualización, en vez de exportarse únicamente como texto. Esto permite operaciones de hoja de cálculo sobre ellas.

### ¿Por qué no usar una sola clase con un `switch`?

Un `switch` pequeño no es automáticamente incorrecto. Sin embargo, aquí las implementaciones ya contienen manejo de recursos, paginación gráfica, hojas, estilos y bibliotecas distintas. Separarlas permite probar y modificar una sin reescribir la otra.

Esa es la justificación **técnica**, no simplemente la existencia de un criterio formativo sobre GoF.

---

## 8. Spring inyecta las estrategias disponibles

Las dos clases concretas tienen `@Component`, por lo que Spring las registra como beans.

El Service recibe una lista de implementaciones:

**Código real — [ReportExportService.java](../../src/main/java/com/petmatch/community/service/report/ReportExportService.java):**

```java
private final Map<ReportExportFormat, ReportExporter> exporters =
        new EnumMap<>(ReportExportFormat.class);

public ReportExportService(
        SupportRequestReportRepository repository,
        List<ReportExporter> availableExporters) {

    this.repository = repository;

    for (var exporter : availableExporters) {
        if (exporters.putIfAbsent(exporter.format(), exporter) != null)
            throw new IllegalStateException("Duplicate exporter");
    }

    if (exporters.size() != ReportExportFormat.values().length)
        throw new IllegalStateException("Missing exporter");
}
```

### ¿Qué aporta Spring aquí?

El contenedor resuelve las implementaciones de `ReportExporter` y las entrega al constructor. El Service no crea manualmente:

```java
new PdfReportExporter()
new ExcelReportExporter()
```

En lugar de eso, depende del **contrato**.

El `EnumMap` permite buscar la implementación por formato. Además, el constructor detecta dos errores de configuración:

- dos implementaciones que declaran el mismo formato;
- ausencia de una implementación para alguno de los valores de `ReportExportFormat`.

Estos fallos se detectan al construir el bean, no después de descargar un archivo incorrecto.

**Matiz:** inyectar una `List<ReportExporter>` es una capacidad de resolución de dependencias de Spring. **No significa que Spring aplique automáticamente Strategy a todos los servicios.** El patrón surge de la decisión de diseño de nuestra aplicación.

---

## 9. El Service coordina la exportación y controla su alcance

**Código real — [ReportExportService.export(...)](../../src/main/java/com/petmatch/community/service/report/ReportExportService.java):**

```java
public ExportFile export(
        SupportRequestReportFilter filter,
        ReportExportFormat format) {

    filter.validate();
    var exporter = exporters.get(Objects.requireNonNull(format));

    if (exporter == null)
        throw new IllegalArgumentException("Unsupported export format");

    var rows = repository.findForExport(filter, MAX_EXPORT_ROWS);

    if (rows.size() > MAX_EXPORT_ROWS)
        throw new ReportExportLimitExceededException(MAX_EXPORT_ROWS);

    try {
        return new ExportFile(
                "petmatch-report." + format.extension(),
                exporter.mediaType(),
                exporter.export(filter, rows, repository.aggregate(filter)));
    } catch (IOException error) {
        throw new UncheckedIOException(
                "Unable to generate report", error);
    }
}
```

El Service está anotado con `@Transactional(readOnly = true)` y establece:

```java
private static final int MAX_EXPORT_ROWS = 3000;
```

### Por qué existe el límite

Una descarga puede consumir memoria para mantener filas, un documento PDF o un libro XLSX. El código actual construye el archivo en memoria y devuelve un `byte[]`. Por eso se restringe la cantidad de registros.

**Código real — [SupportRequestReportRepository.findForExport(...)](../../src/main/java/com/petmatch/community/repository/SupportRequestReportRepository.java):**

```java
public List<ReportRow> findForExport(
        SupportRequestReportFilter filter, int maximumRows) {

    if (maximumRows < 1)
        throw new IllegalArgumentException(
                "Export limit must be positive");

    return entityManager.createQuery(
            detailQuery(entityManager.getCriteriaBuilder(), filter))
            .setMaxResults(Math.addExact(maximumRows, 1))
            .getResultList();
}
```

¿Por qué se consultan **3.001 filas** y no solo 3.000? Porque así se puede detectar si la búsqueda supera el límite. Si el resultado tuviera 3.001 coincidencias, devolver únicamente las primeras 3.000 **sin avisar** produciría una descarga incompleta presentada como completa.

El Controller transforma ese exceso en **HTTP 413** y pide aplicar filtros más restrictivos.

**Limitación del diseño:** el límite de filas no es un límite de tamaño en bytes, tiempo de ejecución ni consumo máximo de memoria. Para volúmenes grandes serían necesarias otras decisiones, como exportación por streaming o procesamiento asíncrono, si los requisitos reales lo justificaran.

---

## 10. El resumen y las filas comparten la misma búsqueda

El Service entrega a cada estrategia:

```java
exporter.export(
        filter,
        rows,
        repository.aggregate(filter));
```

El Repository reutiliza la consulta de detalle y el método común de predicados del capítulo 41. Así se mantiene la semántica de los filtros para el listado y la exportación.

```text
Misma entrada:
  status=OPEN
  supportType=WALK
  fromDate=2030-01-10
  toDate=2030-01-10

Resultado:
  filas que cumplen esos filtros
  resumen de esas mismas condiciones
```

El PDF y Excel reciben **los mismos datos de negocio**. No interpretan por separado qué significa `OPEN`, cómo funciona el intervalo de fechas ni quién es el propietario.

### ¿Existe una garantía absoluta de instantánea?

No. Las filas y el resumen se obtienen mediante consultas separadas. El comportamiento ante cambios concurrentes depende del aislamiento de la base de datos. `@Transactional(readOnly = true)` no equivale por sí solo a una fotografía inmutable.

Si una aplicación necesita exportaciones regulatorias con corte exacto, deberá definir requisitos adicionales de consistencia y reproducibilidad.

---

## 11. Seguridad: descargar datos también requiere autorización

La ruta de exportación está bajo `/admin/**`. PetMatch protege las rutas administrativas mediante `CurrentAdminAuthorization`, que consulta el rol activo persistido además de la autoridad de la sesión.

Esto importa porque la exportación contiene datos de solicitudes de **distintos propietarios**. El hecho de que se entregue un archivo, en vez de una página HTML, no reduce el riesgo de acceso indebido.

En el código actual también encontramos estas defensas:

| Riesgo | Defensa existente |
|---|---|
| Acceso de usuario sin rol administrativo | Autorización de `/admin/**` |
| Filtro con intervalo inválido | Bean Validation y `filter.validate()` |
| Formato desconocido | Enum y respuesta 404 |
| Descarga con más de 3.000 registros | Rechazo explícito con HTTP 413 |
| Caché de respuesta de descarga | Cabecera `Cache-Control: no-store` |
| Texto de Excel que comienza con `=`, `+`, `-` o `@` | Prefijo de apóstrofo en `safeCellText(...)` |

### Inyección de fórmulas en hojas de cálculo

Un título de solicitud es texto proporcionado por usuarios. Si se escribe sin cuidado en una celda, algunas aplicaciones de hoja de cálculo pueden interpretar ciertos prefijos como fórmulas.

**Código real — ExcelReportExporter:**

```java
private String safeCellText(String text) {
    if (text == null) return "";

    var normalized = text.replace('\n', ' ').replace('\r', ' ');

    return !normalized.isEmpty()
            && "=+-@".indexOf(normalized.charAt(0)) >= 0
            ? "'" + normalized
            : normalized;
}
```

Esta defensa reduce riesgos para los valores tratados por ese método. **No debe presentarse como una garantía universal** para cualquier importador o variante de texto malicioso. Conviene ampliar pruebas con tabulaciones, espacios iniciales y consumidores reales si el modelo de amenazas lo exige.

### Cuidado con los datos del archivo

No supongas que un PDF o XLSX está anonimizado. El archivo contiene información de solicitudes y debe manejarse con la misma política de acceso y conservación que el reporte de origen.

---

## 12. Manejo de errores: no todo fallo significa lo mismo

La exportación diferencia varias situaciones:

| Situación | Comportamiento actual |
|---|---|
| Solicitud válida y archivo generado | 200 y bytes del formato solicitado |
| Filtros inválidos en el endpoint | 400 |
| Formato no soportado | 404 |
| Demasiadas filas | 413 |
| Usuario autenticado sin permisos | 403 |
| Usuario no autenticado | Redirección a autenticación en el flujo web |
| `IOException` del generador | Se transforma en `UncheckedIOException` |

La última situación no tiene en este Controller una respuesta personalizada para el usuario. El error se propaga a la infraestructura de manejo de errores del servidor.

**Decisión de diseño:** no ocultar un error de generación produciendo un archivo vacío con respuesta 200. Un archivo corrupto que parece una descarga exitosa es más difícil de diagnosticar que un fallo explícito.

**Pregunta:** ¿por qué una consulta sin coincidencias debe poder producir un archivo válido con cero filas, mientras que una consulta que supera el límite debe rechazarse?

---

## 13. Pruebas de integración: abrir el archivo, no solo verificar su extensión

La clase [ReportExportIntegrationTests.java](../../src/test/java/com/petmatch/community/integration/ReportExportIntegrationTests.java) comprueba el comportamiento HTTP y analiza el contenido real de los archivos.

### 13.1 PDF con filtros y más de una página de resultados

El fixture crea **26 solicitudes**. Veinticinco son de tipo `WALK` y la última es `FEEDING`.

La prueba `pdfExportsFilteredRowsBeyondFirstPage` solicita:

```text
GET /admin/reports/export/pdf
supportType=WALK
grouping=SUPPORT_TYPE
```

Después comprueba:

- respuesta 200;
- `Content-Type: application/pdf`;
- `Content-Disposition` con `petmatch-report.pdf`;
- cabecera binaria `%PDF`;
- contenido legible al abrirlo con PDFBox;
- total filtrado 25;
- presencia de `Pet walk 24`;
- ausencia de `Pet walk 25`;
- resumen `WALK: 25`.

**¿Qué demuestra?** Que la descarga no se limita a la primera página de 20 filas y que los filtros afectan también al contenido generado.

### 13.2 Excel con dos hojas y fechas nativas

La prueba `excelExportsAllRowsAndNativeDates` abre los bytes con `WorkbookFactory` de Apache POI y verifica:

- tipo MIME XLSX;
- hoja `Requests`;
- **27 filas físicas**: una cabecera y 26 solicitudes;
- última solicitud en la fila correspondiente;
- celda de fecha reconocida como fecha formateada;
- hoja `Summary` con total 26.

No basta con que el archivo termine en `.xlsx`: una prueba útil debe **abrirlo y comprobar su estructura**.

### 13.3 Seguridad y entradas incorrectas

`exportRejectsNonAdminInvalidFiltersAndUnknownFormats` comprueba acceso no autorizado, ausencia de sesión, fechas invertidas y formato no soportado.

`reportPageDisplaysBothExportLinks` verifica que la pantalla presenta ambas descargas y conserva un filtro en el enlace.

### ¿Qué no demuestran estas pruebas?

No certifican que el PDF sea visualmente accesible, que todas las fuentes Unicode funcionen, que el archivo sea adecuado para impresión profesional, ni que la exportación soporte millones de registros.

Son pruebas de contrato y contenido para los escenarios definidos. La calidad visual, la accesibilidad documental y el rendimiento requieren verificaciones complementarias.

---

## 14. Cómo ejecutar una demostración de extremo a extremo

### Preparación

Necesitas el proyecto configurado con Java 21, Maven Wrapper y una base de datos de desarrollo. La configuración de conexión está en `src/main/resources/application.yaml` y utiliza:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Para ejecutar la suite de pruebas se emplea una base aislada mediante:

```text
TEST_DB_URL
TEST_DB_USERNAME
TEST_DB_PASSWORD
```

**No uses datos reales ni la base de producción para esta práctica.**

### Demostración manual

1. Inicia la aplicación con la configuración de desarrollo.
2. Accede con una cuenta administrativa autorizada.
3. Abre `/admin/reports`.
4. Aplica un filtro por tipo y otro por fecha.
5. Anota el total filtrado y la agrupación.
6. Descarga PDF y ábrelo con un lector compatible.
7. Descarga XLSX y ábrelo con una aplicación de hojas de cálculo.
8. Comprueba que ambos contienen las mismas solicitudes y el mismo resumen.
9. Cambia el filtro y repite la comparación.
10. Accede con una cuenta sin privilegios administrativos y comprueba que no puede descargar.

### Demostración automatizada

Con una base de tests separada y lista para utilizar:

```bash
./mvnw clean verify
```

Este comando ejecuta la verificación del proyecto según su configuración Maven. El capítulo describe **las pruebas presentes en el repositorio**, no afirma haber ejecutado aquí la suite ni haber medido tiempos de descarga.

---

## 15. Comparar alternativas sin convertir Strategy en una regla universal

| Alternativa | Cuándo tiene sentido | Riesgo si se utiliza mal |
|---|---|---|
| Un `switch` pequeño | Dos formatos triviales con muy poca lógica | Controller o Service crecen con detalles de cada formato |
| Strategy con clases independientes | Algoritmos de representación diferentes, dependencias específicas y pruebas separadas | Exceso de archivos si las diferencias son insignificantes |
| Funciones/lambdas | Transformaciones cortas y sin estado, fáciles de expresar como función | Pérdida de claridad para generación compleja con recursos y errores |
| Servicio externo de generación | Alto volumen, plantillas especializadas o requisitos operativos concretos | Complejidad de red, costos y despliegue innecesarios para un proyecto pequeño |

**PetMatch elige Strategy para exportar, pero no para agrupar SQL.** Es una distinción deliberada: en el capítulo 41 solo cambiaba el atributo del `GROUP BY`; aquí cambian bibliotecas, estructuras de datos y procesos de escritura.

### ¿Qué patrones se encuentran dentro de Spring?

Al estudiar el framework reconocerás ideas relacionadas con patrones conocidos:

- **Proxy:** Spring AOP y algunas funcionalidades transaccionales utilizan proxies.
- **Factory / Factory Method:** el contenedor construye y suministra objetos administrados mediante mecanismos de creación.
- **Template Method:** algunas abstracciones de infraestructura encapsulan pasos repetidos y permiten personalización.
- **Observer:** el mecanismo de eventos y listeners de Spring presenta relaciones conceptuales con publicación y suscripción.

**Matiz esencial:** que una característica de Spring recuerde un patrón no implica que todas sus implementaciones sean una reproducción literal del ejemplo GoF. Tampoco significa que agregar interfaces y componentes sea obligatorio en cada caso.

Para comprender Strategy, primero identifica una **variación real del algoritmo**. Después evalúa si una interfaz mejora el diseño.

---

## 16. Límites y posibles mejoras del diseño actual

El módulo es funcional y su alcance está delimitado. Una evaluación técnica debe reconocer también lo que **no** resuelve todavía.

### 16.1 PDF y texto internacional

Las fuentes estándar usadas por el exportador no cubren todos los caracteres Unicode. El código sustituye los caracteres no admitidos y recorta cadenas que exceden su ancho.

Si el producto requiere nombres internacionales completos o documentos accesibles, sería razonable estudiar fuentes embebidas compatibles, ajuste de texto y pruebas visuales. **No afirmes que esas mejoras ya existen.**

### 16.2 Archivos construidos en memoria

El Service devuelve `byte[]` y los exportadores utilizan `ByteArrayOutputStream`. Con un límite de 3.000 registros, la implementación controla el volumen de filas, pero no el tamaño exacto del archivo.

Para cargas mayores podrían considerarse técnicas de streaming o trabajos asíncronos. No deben añadirse sin medir primero la necesidad.

### 16.3 Reproducibilidad de reportes

El nombre del archivo es fijo:

```text
petmatch-report.pdf
petmatch-report.xlsx
```

No incluye identificador de consulta, fecha de generación ni versión del conjunto de datos. Si una organización requiere trazabilidad documental, habría que definir esos metadatos y su política.

### 16.4 Pruebas adicionales útiles

Podrían incorporarse casos de:

- cero registros;
- exactamente 3.000 y más de 3.000 registros;
- textos con caracteres Unicode no admitidos por la fuente PDF;
- textos largos que requieren tratamiento especial;
- celdas con prefijos de fórmulas y espacios o tabulaciones iniciales;
- errores de escritura del archivo;
- descargas simultáneas y consumo de memoria.

Estos son **escenarios recomendados**, no pruebas que el capítulo atribuya falsamente al repositorio.

---

## 17. ⚠️ Errores frecuentes

### Error A — Exportar únicamente la página visible

Una página de 20 filas no equivale al total de coincidencias. Utiliza una consulta de exportación con límite explícito.

### Error B — Repetir consultas dentro de cada exportador

El PDF y Excel deben recibir los datos preparados. No deberían decidir de manera independiente cómo se aplican los filtros de negocio.

### Error C — Escribir un archivo sin verificarlo

Una respuesta HTTP 200 y una extensión `.pdf` o `.xlsx` no prueban que el archivo sea válido. Ábrelo con la biblioteca correspondiente y comprueba su contenido.

### Error D — Usar un tipo MIME genérico para todos los formatos

PDF y XLSX tienen tipos de contenido diferentes. La respuesta debe comunicar el formato que realmente produce.

### Error E — Silenciar un exceso de registros

Recortar 3.001 coincidencias a 3.000 y entregar el archivo como si estuviera completo es engañoso. El límite debe producir un rechazo explícito o un contrato de exportación parcial claramente identificado.

### Error F — Creer que un enlace oculto protege una descarga

La seguridad debe comprobarse en el servidor. Cualquier cliente HTTP puede construir una URL, incluso si el botón no aparece en su pantalla.

### Error G — Llamar Strategy a cualquier `if` o `switch`

El patrón aporta valor cuando encapsula algoritmos intercambiables. Cambiar solamente el nombre de una columna no exige crear una jerarquía de estrategias.

### Error H — Declarar el PDF accesible por el solo hecho de abrirse

Que un lector muestre el documento no implica que tenga estructura semántica, etiquetas accesibles, lectura correcta con tecnologías de asistencia o cobertura Unicode suficiente.

---

## 18. 🔁 Llévalo a tu proyecto

**Objetivo:** diseñar una exportación útil para un dominio diferente, sin copiar las entidades de PetMatch.

Escoge una aplicación propia: préstamos, reservas, inventarios, ventas o asistencia a eventos.

1. **Define el caso de uso.** ¿Quién necesita descargar información y para qué?
2. **Delimita los datos.** Elige una proyección con campos concretos y define filtros verificables.
3. **Separa responsabilidades.** Distingue selección de datos, validación, generación del archivo y respuesta HTTP.
4. **Justifica los formatos.** Explica qué cambia realmente entre un documento para lectura y un archivo para análisis tabular.
5. **Diseña el contrato.** Propón una interfaz de exportación únicamente si hay algoritmos diferentes que la justifican.
6. **Define los límites.** Decide cómo actuarás ante una exportación demasiado grande y por qué.
7. **Incluye seguridad.** Determina quién puede descargar datos globales y cómo se verifica ese permiso en el servidor.
8. **Planifica pruebas.** Define cómo comprobarás la validez interna del PDF o XLSX, los filtros y las respuestas de error.

### Actividad de diseño: ¿Strategy o solución directa?

Compara estas situaciones:

| Necesidad | Pregunta para decidir |
|---|---|
| Agrupar solicitudes por estado o tipo | ¿Cambia el algoritmo o solo un atributo SQL? |
| Exportar PDF o XLSX | ¿Cambian estructuras, bibliotecas y tratamiento de recursos? |
| Elegir separador coma o punto y coma en CSV | ¿La diferencia justifica otra clase completa? |
| Enviar notificaciones por correo o SMS | ¿Existen protocolos, dependencias y fallos distintos? |

No hay una cantidad obligatoria de clases. La evidencia esperada es una **decisión explicada** que respete SOLID, DRY y KISS sin sobreingeniería.

---

## 19. 🧪 Comprueba que entendiste

Responde antes de volver al código:

1. ¿Por qué una exportación no debe depender de la página actualmente visible?
2. ¿Qué información comparten el PDF y el Excel?
3. ¿Qué algoritmo específico implementa `PdfReportExporter`?
4. ¿Qué estructura produce `ExcelReportExporter`?
5. ¿Cuál es la responsabilidad de `ReportExporter`?
6. ¿Por qué `ReportExportService` recibe `List<ReportExporter>`?
7. ¿Qué detecta `putIfAbsent` en el constructor del Service?
8. ¿Por qué se consultan 3.001 filas si el máximo es 3.000?
9. ¿Qué significa HTTP 413 en este endpoint?
10. ¿Qué diferencia existe entre `Content-Type` y `Content-Disposition`?
11. ¿Por qué una descarga requiere autorización incluso si no modifica datos?
12. ¿Qué problema intenta mitigar `safeCellText(...)`?
13. ¿Qué prueba muestra que el PDF respeta filtros más allá de la primera página?
14. ¿Qué prueba comprueba que Excel contiene fechas nativas?
15. ¿Qué limitación presenta el PDF actual con caracteres Unicode?
16. ¿Por qué `@Transactional(readOnly = true)` no garantiza por sí solo una instantánea inmutable?
17. ¿Por qué la agrupación SQL del capítulo 41 no usa Strategy, pero la exportación sí?
18. ¿Qué modificarías primero si el negocio necesitara exportar cientos de miles de filas?

### Desafío de razonamiento

Hay 26 solicitudes: 25 `WALK` y una `FEEDING`. El usuario filtra por `WALK` y agrupa por `SUPPORT_TYPE`.

Explica:

- cuántas filas aparecerían en la primera página HTML;
- cuántas solicitudes debería incluir la descarga;
- qué total debería figurar en el archivo;
- qué grupo debería aparecer en el resumen;
- qué respondería el servidor si una búsqueda devolviera 3.001 filas;
- qué ocurriría si el solicitante no tuviera permisos administrativos.

Después localiza en `ReportExportIntegrationTests` las comprobaciones que respaldan las respuestas.

---

## ✅ Qué debes recordar

**Strategy no se introduce para demostrar que conocemos patrones: se introduce cuando existen algoritmos realmente intercambiables y conviene aislar sus diferencias.**

En PetMatch, PDF y XLSX representan dos procesos de generación distintos. El Repository selecciona la población, el Service aplica límites y elige una estrategia, cada exportador produce su archivo y el Controller construye una respuesta HTTP coherente.

La calidad de esta solución no depende del número de clases. Depende de que los filtros sean consistentes, las descargas sean seguras, los archivos se puedan abrir, los errores no se oculten y las decisiones se puedan explicar.

### Para continuar

- [40 — Administración de roles, autorización y auditoría](40-roles-autorizacion-y-auditoria.md).
- [41 — Reportes dinámicos con JPA: filtros, proyecciones y agrupaciones](41-reportes-dinamicos-jpa.md).
- [14 — Transacciones y consistencia](../02-dominio-y-persistencia/14-transacciones-y-consistencia.md).
- [25 — Autorización y ownership](../04-seguridad/25-autorizacion-y-ownership.md).
- [31 — Pruebas unitarias](../06-calidad-y-recorrido/31-pruebas-unitarias.md).
- [32 — Pruebas de integración](../06-calidad-y-recorrido/32-pruebas-de-integracion.md).

### Referencias técnicas

- [Spring Framework — Inyección de dependencias](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html).
- [Spring Framework — Controladores web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html).
- [Spring Security — Autorización de peticiones](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).
- [Apache PDFBox — Documentación](https://pdfbox.apache.org/).
- [Apache POI — Documentación](https://poi.apache.org/).
