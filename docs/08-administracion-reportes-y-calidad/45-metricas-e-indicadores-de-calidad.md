# 45 — Métricas e indicadores de calidad: medir, interpretar y conservar evidencias

En el [capítulo 44](44-modelos-de-calidad-del-software.md) aprendimos a distinguir las **características del producto**, la **gestión de calidad** y los **procesos de mejora**. Un modelo como ISO/IEC 25010:2023 ayuda a identificar qué propiedades importan. Pero todavía necesitamos resolver una pregunta fundamental:

> **¿Cómo comprobamos esas propiedades con datos confiables, sin confundir el número de pruebas aprobadas, la cobertura de código, el rendimiento y la experiencia real de las personas?**

No alcanza con afirmar «hay 100 % de calidad» porque la compilación terminó correctamente. Tampoco sería válido reutilizar porcentajes obtenidos meses atrás para describir una versión distinta de la aplicación.

En este capítulo vamos a seguir las **fuentes reales de evidencia de PetMatch Community**: Maven, JUnit, los reportes XML de Surefire, JaCoCo, las pruebas de integración, una medición reproducible de reportes, comprobaciones estáticas de accesibilidad y GitHub Actions. Aprenderemos qué mide cada una, qué deja por fuera y cómo documentar el resultado sin inventar cifras.

El propósito no es convertir el libro en un informe de cumplimiento de un cronograma. **PetMatch es un caso de estudio**. El método podrá utilizarse después en una biblioteca, un sistema de reservas, una aplicación de inventario o cualquier otro proyecto.

Al finalizar podrás:

- diferenciar **característica de calidad**, **criterio**, **métrica**, **indicador**, **meta**, **línea base** y **evidencia**;
- calcular e interpretar resultados de pruebas sin esconder fallos, errores ni pruebas omitidas;
- explicar el significado y los límites de la cobertura de líneas y ramas;
- localizar la configuración real de Surefire y JaCoCo y los archivos que producen;
- distinguir métricas de seguridad, adecuación funcional, confiabilidad, interacción y rendimiento;
- leer el procedimiento de medición de latencia del repositorio y sus condiciones;
- reconocer por qué un script estático o un test de navegador no reemplaza una evaluación humana;
- conservar versión, entorno, método y fuente para permitir comparaciones legítimas;
- formular una matriz pequeña de métricas transferible a un proyecto propio.

---

## 1. Medir comienza con una pregunta, no con una cifra

Imagina que un equipo muestra estos resultados:

```text
Tests: 100 %
Coverage: 85 %
Performance: 20 ms
Accessibility: 100 %
```

Parecen números positivos, pero todavía no podemos interpretar su significado.

¿Qué significa «Tests: 100 %»? ¿Se ejecutaron tres pruebas o trescientas? ¿Había pruebas omitidas? ¿«Coverage» indica líneas, ramas o métodos? ¿Los 20 milisegundos corresponden a una petición HTTP completa, una operación del Service o una consulta SQL? ¿La supuesta accesibilidad se evaluó con personas o se comprobó un conjunto pequeño de atributos HTML?

### La pregunta correcta

En vez de comenzar por la herramienta, plantea:

| Pregunta | Evidencia posible | Qué debes conocer |
|---|---|---|
| ¿La suite automatizada pasó? | Reportes Surefire | Pruebas, fallos, errores y omitidas |
| ¿Qué código ejecutaron esas pruebas? | Reporte JaCoCo | Cobertura, exclusiones y denominador |
| ¿Los filtros funcionan en los límites? | Prueba de integración | Datos de frontera y resultado esperado |
| ¿Una cuenta normal puede cambiar roles? | Prueba negativa HTTP | Identidad, respuesta y estado persistido |
| ¿Cuánto tarda una operación? | Medición repetida | Operación, datos, muestras, entorno |
| ¿Una persona comprende el formulario? | Evaluación de tareas | Contexto, participantes, observaciones |

**🧠 Idea mental**

```text
Pregunta de calidad
        ↓
Definición del comportamiento deseado
        ↓
Métrica o criterio observable
        ↓
Fuente de datos y método de medición
        ↓
Resultado con contexto y versión
        ↓
Interpretación y decisión
```

**Antes de continuar:** ¿qué información adicional pedirías para interpretar «El reporte tarda 15 ms»?

---

## 2. Vocabulario: concepto, requisito, métrica, indicador y meta

Estas palabras suelen utilizarse como sinónimos, pero cumplen funciones diferentes.

| Término | Significado práctico | Ejemplo |
|---|---|---|
| **Característica de calidad** | Propiedad del producto que importa | Seguridad |
| **Requisito de calidad** | Necesidad que el producto debe satisfacer | Solo administradores activos cambian roles |
| **Criterio de aceptación** | Condición observable para decidir si se satisface el requisito | Un usuario normal obtiene acceso denegado y el rol no cambia |
| **Métrica** | Medida definida mediante una unidad, un conteo o una fórmula | Cantidad de casos negativos aprobados |
| **Indicador** | Interpretación de una o más medidas para apoyar una decisión | Progreso de verificación de controles críticos |
| **Meta o umbral** | Valor o condición acordada antes de evaluar | Ningún caso crítico de autorización falla |
| **Línea base** | Resultado inicial obtenido bajo condiciones conocidas | Primera medición reproducible de la operación |
| **Evidencia** | Registro que permite comprobar qué ocurrió | XML de Surefire, reporte JaCoCo, resultado de una sesión |
| **Resultado observado** | Valor o comportamiento efectivamente obtenido | Se registran los datos de una ejecución identificada |

Una **meta** no es una medición. Decidir que queremos un p95 inferior a cierto presupuesto no prueba que el programa ya lo alcance.

Una **línea base** tampoco es necesariamente una meta. Es un punto de comparación que puede mostrar que hay mucho por mejorar.

### Ejemplo aplicado

**Necesidad:** un reporte debe contar todas las solicitudes filtradas, incluso si la tabla está paginada.

**Criterio:** con 26 solicitudes coincidentes y tamaño de página 20, el total debe indicar 26.

**Evidencia:** [ReportIntegrationTests.totalsAreNotLimitedToPage()](../../src/test/java/com/petmatch/community/integration/ReportIntegrationTests.java).

**Conclusión válida:** existe una prueba de integración diseñada para ese comportamiento.

**Conclusión inválida:** «Todos los reportes del sistema son perfectos porque esa prueba existe».

---

## 3. Relacionar las métricas con un modelo de calidad

Las métricas tienen sentido cuando ayudan a comprobar una propiedad relevante del producto. El [capítulo 44](44-modelos-de-calidad-del-software.md) estudió el modelo de nueve características de **ISO/IEC 25010:2023**.

Podemos formular una matriz inicial:

| Característica | Riesgo principal | Evidencia que puede aportar valor |
|---|---|---|
| Adecuación funcional | Un filtro devuelve registros equivocados | Casos positivos, negativos y de frontera |
| Seguridad | Una cuenta no autorizada modifica datos | Pruebas de autorización y estado persistido |
| Confiabilidad | Dos operaciones dejan estados incompatibles | Prueba de concurrencia y resultado final |
| Mantenibilidad | Una modificación obliga a alterar capas ajenas | Pruebas, revisión de dependencias y costo de cambio |
| Eficiencia del desempeño | Una consulta se vuelve demasiado lenta | Mediana, p95, plan SQL y carga definida |
| Capacidad de interacción | El usuario no puede completar una tarea | Verificación técnica y evaluación humana |
| Compatibilidad | Un archivo no puede abrirse correctamente | Lectura del PDF/XLSX con un consumidor conocido |

**Atención:** esta relación es orientadora. Una cobertura alta de pruebas **no es una medida directa de mantenibilidad**, y una única latencia **no resume toda la eficiencia del producto**. Las características requieren criterios y métodos de evaluación apropiados.

**⚠️ Error frecuente:** escoger una métrica porque es fácil de mostrar en una gráfica y después atribuirle una propiedad que realmente no mide.

---

## 4. La unidad de una medición necesita un denominador claro

Un porcentaje es una **proporción entre cantidades compatibles**.

Por ejemplo:

```text
percentage = 100 × numerator / denominator
```

Antes de calcularlo debemos definir:

1. ¿Qué representa el numerador?
2. ¿Qué representa el denominador?
3. ¿A qué versión del producto corresponde?
4. ¿Cuáles elementos se incluyen o excluyen?
5. ¿Qué significa un resultado bueno o malo?

### Ejemplo hipotético — no es una medición de PetMatch

Un equipo definió 12 criterios técnicos y verificó satisfactoriamente 9.

```text
verifiedCriteria = 9
definedCriteria = 12
verificationRate = 100 × 9 / 12 = 75 %
```

Este 75 % significa **9 criterios verificados de 12 definidos**. No significa que el 75 % del software funcione ni que el 75 % de sus usuarios esté satisfecho.

### Cuando el denominador es cero

Si todavía no existe una muestra o no se ejecutó ninguna prueba, el resultado **no debe convertirse silenciosamente en 0 % o 100 %**.

Escribe:

```text
Status: Not measured
Reason: No observations available
```

La ausencia de datos no es un resultado de calidad. Es una actividad pendiente.

---

## 5. ¿Qué produce Maven cuando ejecuta las pruebas?

PetMatch utiliza **Java 21**, **Spring Boot 4.1.1** y Maven Wrapper. La configuración está en [pom.xml](../../pom.xml).

En el proyecto, el plugin **Maven Surefire** ejecuta las pruebas correspondientes a la fase `test`. JaCoCo instrumenta la ejecución y produce un reporte durante `verify`.

**Código real — fragmento de `pom.xml`:**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.14</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

El proyecto también declara `maven-surefire-plugin`, configurado para activar el perfil de pruebas.

### Secuencia conceptual

```text
Código y pruebas
      |
      v
Maven clean verify
      |
      +--> Compilación
      +--> Ejecución de pruebas con Surefire
      +--> Instrumentación y recolección JaCoCo
      +--> Verificación del build y reporte de cobertura
      |
      v
Archivos de resultados en target/
```

**¿Por qué se utiliza `verify`?** Porque en este `pom.xml` el objetivo de reporte de JaCoCo está vinculado a la fase `verify`. Ejecutar únicamente `test` no debe confundirse con haber generado necesariamente el informe HTML/XML completo de esa fase.

**🔎 Archivos del proyecto:**

- [pom.xml](../../pom.xml).
- [GitHub Actions — ci.yml](../../.github/workflows/ci.yml).

Los resultados de una ejecución se generan normalmente bajo `target/`, que es una carpeta de build, no una fuente editorial del libro.

---

## 6. Surefire: contar pruebas, fallos, errores y omisiones

Los reportes de Surefire se guardan normalmente en:

```text
target/surefire-reports/
```

Su formato incluye archivos `TEST-*.xml` que describen las suites de pruebas ejecutadas.

### Los cuatro campos que debemos distinguir

| Campo | Qué representa |
|---|---|
| `tests` | Total de casos contabilizados en la suite |
| `failures` | Casos con aserciones que fallaron |
| `errors` | Casos que terminaron con errores inesperados durante su ejecución |
| `skipped` | Casos omitidos o no ejecutados según el reporte |

El conjunto de **pruebas aprobadas** puede calcularse como:

```text
passed = tests - failures - errors - skipped
```

Para presentar una proporción, define primero el denominador.

### Dos preguntas distintas

**A. ¿Qué proporción de los casos contabilizados terminó aprobada?**

```text
passRateOfReportedTests = 100 × passed / tests
```

**B. ¿Qué proporción de los casos realmente ejecutados terminó aprobada?**

```text
executed = tests - skipped
passRateOfExecutedTests = 100 × passed / executed
```

La segunda excluye los omitidos del denominador. Eso **no significa** que los casos omitidos deban desaparecer del informe.

### Ejemplo hipotético

```text
tests = 20
failures = 1
errors = 1
skipped = 2

passed = 20 - 1 - 1 - 2 = 16
executed = 20 - 2 = 18

passRateOfReportedTests = 80 %
passRateOfExecutedTests = 88.89 %
```

Ambas cifras son matemáticamente válidas, pero **responden preguntas diferentes**. Es indispensable informar los conteos originales.

En caso de cero pruebas o cero ejecutadas, marca la tasa correspondiente como **no aplicable** en vez de dividir por cero.

### ¿Qué indica una ejecución verde?

Que el proceso configurado se completó satisfactoriamente para los casos que ejecutó. No demuestra automáticamente:

- cobertura de todas las funciones;
- ausencia de vulnerabilidades;
- ausencia de defectos de concurrencia no reproducidos;
- aceptación por un usuario real;
- rendimiento bajo carga productiva.

**🔎 Caso PetMatch:** [AdminIntegrationTests](../../src/test/java/com/petmatch/community/integration/AdminIntegrationTests.java) contiene pruebas para accesos administrativos, CSRF, roles y sesiones desactualizadas. El porcentaje total no reemplaza la lectura de **qué comportamiento comprueba cada prueba**.

---

## 7. Contar pruebas no es lo mismo que cubrir riesgos

Imagina dos suites de igual tamaño:

**Suite A:** 50 pruebas comprueban variantes de formato de un nombre.

**Suite B:** 10 pruebas comprueban permisos, límites de fecha, invariantes y fallos críticos.

¿Podemos concluir que A ofrece más confianza únicamente porque contiene más pruebas? No.

La importancia depende de **qué riesgo comprueba cada caso**, qué datos utiliza y qué resultado observa.

### Diferenciar niveles de prueba

PetMatch tiene varios tipos de pruebas:

| Ejemplo | Frontera que se intenta comprobar |
|---|---|
| [SupportRequestServiceTests](../../src/test/java/com/petmatch/community/service/SupportRequestServiceTests.java) | Reglas del Service con colaboradores sustituidos |
| [ReportExportServiceTests](../../src/test/java/com/petmatch/community/service/ReportExportServiceTests.java) | Selección de exportadores y límites del orquestador |
| [ReportIntegrationTests](../../src/test/java/com/petmatch/community/integration/ReportIntegrationTests.java) | Service, Criteria API, JPA y base de pruebas |
| [ReportExportIntegrationTests](../../src/test/java/com/petmatch/community/integration/ReportExportIntegrationTests.java) | HTTP, seguridad y contenido de archivos generados |
| [RoleConcurrencyTests](../../src/test/java/com/petmatch/community/integration/RoleConcurrencyTests.java) | Transacciones y dos cambios de rol simultáneos |
| [WebRegressionTests](../../src/test/java/com/petmatch/community/integration/WebRegressionTests.java) | Sesión web, CSRF y acceso de cuentas desactivadas |

La variedad de fronteras responde a riesgos distintos. No todos necesitan pruebas del mismo nivel ni la misma cantidad de repeticiones.

**🧠 Idea mental**

```text
Cantidad de pruebas
    ≠
Cantidad de requisitos
    ≠
Cobertura de código
    ≠
Ausencia de defectos
```

---

## 8. JaCoCo: qué significa la cobertura de líneas

JaCoCo registra qué partes del código instrumentado se ejecutaron durante las pruebas.

En este proyecto se utiliza **JaCoCo 0.8.14**. Sus reportes incluyen un contador `LINE` con valores `covered` y `missed`.

### Fórmula de cobertura de líneas

```text
lineCoverage = 100 × coveredLines / (coveredLines + missedLines)
```

**Ejemplo hipotético — no es un resultado actual de PetMatch:**

```text
coveredLines = 72
missedLines = 18
lineCoverage = 100 × 72 / (72 + 18) = 80 %
```

Esta cifra describe la relación entre líneas consideradas cubiertas y no cubiertas según el reporte y el alcance instrumentado.

### ¿De dónde sale el dato?

En un resultado JaCoCo XML se puede encontrar un contador como el siguiente:

```xml
<counter type="LINE" missed="18" covered="72"/>
```

El fragmento es **ilustrativo**: no se extrajo de una ejecución actual del proyecto.

### Archivos que genera la configuración

```text
target/site/jacoco/index.html
target/site/jacoco/jacoco.xml
```

- El **HTML** permite explorar resultados por paquetes y clases.
- El **XML** resulta adecuado para automatizar la lectura de contadores.

**Pregunta:** ¿qué ocurre con la fórmula si `covered + missed` es cero? La respuesta correcta es «no aplicable» para esa población, no «100 % por defecto».

---

## 9. Líneas, ramas y métodos: varias coberturas, no una cifra universal

JaCoCo puede informar varios tipos de contador, entre ellos:

- `LINE`: líneas;
- `BRANCH`: ramas de decisión;
- `METHOD`: métodos;
- `CLASS`: clases;
- `INSTRUCTION`: instrucciones instrumentadas;
- `COMPLEXITY`: complejidad asociada al flujo.

No deben mezclarse sus denominadores.

### Un ejemplo de decisión

```java
public boolean canEdit(boolean owner, boolean active) {
    return owner && active;
}
```

Una prueba que llama a `canEdit(true, true)` puede ejecutar la línea del `return`. Sin embargo, no necesariamente demuestra el comportamiento de estas otras entradas:

```text
owner=true,  active=false
owner=false, active=true
owner=false, active=false
```

Por eso **cobertura de línea no significa cobertura de todas las combinaciones de condiciones**.

### Tampoco es una medida directa de corrección

Una prueba puede ejecutar una línea, pero no verificar correctamente su resultado. Un test que llama a un método sin una aserción útil puede aumentar cobertura sin aportar la confianza necesaria.

**⚠️ Error frecuente:** celebrar un porcentaje alto de JaCoCo y dejar sin comprobar el cambio de roles por un usuario no autorizado.

### ¿Es obligatorio llegar al 100 %?

No existe un umbral universal de cobertura que garantice calidad. Un objetivo interno puede ser útil si se define su alcance y se combina con pruebas sobre los riesgos importantes. El equipo debería explicar **por qué eligió el umbral** y qué significa realmente su cumplimiento.

En el capítulo 44 vimos que calidad y mantenibilidad son más amplias que un porcentaje de ejecución instrumentada.

---

## 10. Cómo interpretar una cobertura histórica sin presentarla como actual

El repositorio conserva una matriz antigua en `docs/08-aplicacion-cronograma/calidad/02-matriz-metricas.md`.

Ese documento identifica una ejecución del **20 de octubre de 2026** con un commit de referencia y un entorno determinados. Sus cifras pertenecen a **esa fotografía histórica**.

Entre sus resultados se registró una cobertura de líneas de **531 de 897, aproximadamente 59,2 %** para el alcance instrumentado en aquel momento.

### Qué podemos afirmar

> En el registro histórico de esa ejecución se informó una cobertura global de líneas aproximada de 59,2 %.

### Qué no podemos afirmar

> La versión actual de PetMatch tiene una cobertura global de 59,2 %.

El código siguió cambiando después: aparecieron nuevas pruebas, control de sesiones desactivadas, exportadores, mediciones y ajustes de concurrencia.

Además, ese registro histórico menciona clases de agrupación como `StatusReportAggregationStrategy` que **ya no forman parte de la implementación vigente**. La agrupación actual utiliza Criteria API con un enum, según el capítulo 41.

### ¿Cuál es la decisión editorial?

La documentación pedagógica explica **cómo medir** y **cómo interpretar**. Los resultados variables deben conservarse como registros de una **versión y ejecución concretas**, no como verdades permanentes del libro.

**No modifiques o sobrescribas la evidencia histórica para que parezca actual.** Si quieres comparar, conserva ambas ejecuciones separadas y documenta qué cambió.

---

## 11. Trazabilidad: un requisito debe tener una prueba pertinente

La cobertura responde qué código se ejecutó. La **trazabilidad de requisitos** responde una pregunta distinta:

> ¿Qué requisito o riesgo está relacionado con qué criterio y qué evidencia lo comprueba?

### Matriz conceptual

| Necesidad | Criterio observable | Prueba de PetMatch |
|---|---|---|
| Un usuario normal no cambia roles | La petición se deniega y el rol permanece igual | `userCannotReadOrChangeThirdPartyRoles` |
| El último administrador sigue existiendo | Se rechaza la última degradación | `lastAdminCannotBeDemoted` |
| Las fechas del reporte incluyen el último día completo | Se incluyen registros del día final y se excluye la medianoche siguiente | `dateBoundsIncludeWholeLastDayAndExcludeNextMidnight` |
| La paginación no falsea el total | Con 26 coincidencias, el total sigue siendo 26 | `totalsAreNotLimitedToPage` |
| PDF conserva los filtros más allá de la primera página | El archivo contiene todas las coincidencias admitidas | `pdfExportsFilteredRowsBeyondFirstPage` |

**🔎 Archivos correspondientes:**

- [AdminIntegrationTests.java](../../src/test/java/com/petmatch/community/integration/AdminIntegrationTests.java).
- [ReportIntegrationTests.java](../../src/test/java/com/petmatch/community/integration/ReportIntegrationTests.java).
- [ReportExportIntegrationTests.java](../../src/test/java/com/petmatch/community/integration/ReportExportIntegrationTests.java).

### Un indicador posible

Si el equipo identifica 10 criterios críticos y ejecuta pruebas válidas para 8, puede registrar:

```text
verifiedCriteria = 8
definedCriticalCriteria = 10
verificationProgress = 80 %
```

Esto es un **ejemplo hipotético**, no una medición actual del proyecto.

La interpretación correcta sería «8 de 10 criterios críticos definidos cuentan con verificación». **No** equivale a «80 % de requisitos del negocio aceptados por el cliente».

### Cuándo una matriz engaña

Si se marca un requisito como comprobado porque existe una prueba con nombre parecido, pero esa prueba no verifica el estado persistido o el caso negativo relevante, la trazabilidad es solo aparente.

Debes revisar **el comportamiento real que observa la prueba**.

---

## 12. Medir defectos: severidad, estado y evidencia de corrección

Una lista de defectos es útil cuando permite priorizar y comprobar mejoras, no cuando solo busca reducir una cifra.

### Información mínima de un hallazgo

| Dato | Propósito |
|---|---|
| Identificador | Diferenciar el hallazgo de otros |
| Versión y entorno | Saber dónde se observó |
| Pasos para reproducir | Permitir repetirlo |
| Resultado esperado | Explicar el criterio incumplido |
| Resultado observado | Describir el defecto sin interpretaciones vagas |
| Impacto y severidad | Priorizar por riesgo |
| Cambio realizado | Relacionar la intervención con el defecto |
| Comprobación posterior | Mostrar que se volvió a ejecutar el caso |
| Estado | Abierto, corregido técnicamente, pendiente de validación, etc. |

### Severidad y prioridad no son equivalentes

- **Severidad:** consecuencia del defecto si ocurre.
- **Prioridad:** orden en el que el equipo decide atenderlo considerando riesgo, exposición y restricciones.

Un defecto grave de autorización normalmente exige una atención alta. Una mejora visual menor puede quedar detrás de una invariante de negocio rota.

### Indicadores útiles, pero limitados

```text
identifiedDefects = number of recorded defects
verifiedFixes = number of fixes that passed their regression check
openDefects = number of unresolved records
```

El número de defectos registrados depende de **cuánto y cómo se buscó**. Encontrar más defectos durante una auditoría profunda no demuestra por sí solo que esa versión sea peor que otra menos evaluada.

**⚠️ Error frecuente:** declarar «cero defectos» porque la suite está verde. Esa ejecución demuestra ausencia de fallos en los casos ejecutados, no ausencia universal de errores.

---

## 13. Accesibilidad técnica: contar asociaciones no equivale a validar personas

El [capítulo 43](43-usabilidad-navegacion-y-accesibilidad.md) explicó cómo se relacionan controles de formulario y mensajes mediante `aria-describedby`, `aria-invalid` e identificadores HTML.

PetMatch tiene un script de comprobación estática: [tools/check-accessibility.py](../../tools/check-accessibility.py).

**Código real — fragmento del script:**

```python
for field in re.findall(r'th:errors="\*\{(\w+)\}"', text):
    count += 1
    if f"'{field}-error'" not in text or f'id="{field}-error"' not in text:
        missing.append(path + ':' + field)
```

El script revisa **cuatro plantillas** de formularios; cuenta los campos con `th:errors` que reconoce y detecta asociaciones textuales faltantes.

### Qué se puede calcular

Si el script revisó 14 campos en una versión y ninguno aparece como faltante, una forma de expresar el control es:

```text
checkedFields = 14
missingAssociations = 0
associatedFields = checkedFields - missingAssociations

staticAssociationRate = 100 × associatedFields / checkedFields
```

**Este es un ejemplo de interpretación del alcance del script**, no un porcentaje de accesibilidad global ni un resultado que estemos midiendo de nuevo en este capítulo.

### Qué no prueba ese porcentaje

No demuestra:

- contraste suficiente en todos los estados;
- orden correcto del foco con teclado;
- funcionamiento completo de un lector de pantalla;
- contenido alternativo correcto de todos los elementos;
- comprensión real de los mensajes por una persona;
- conformidad integral con WCAG 2.2 AA.

Para esas conclusiones se requieren criterios y procedimientos de evaluación apropiados.

**🔁 Regla transferible:** escribe «asociaciones estáticas verificadas en cuatro plantillas» en vez de «la aplicación es 100 % accesible».

---

## 14. Interacción y usabilidad: métricas que requieren observación humana

Una necesidad como «el usuario puede completar su tarea» no se mide únicamente leyendo el HTML.

### Indicadores posibles

| Pregunta | Medida posible | Condición necesaria |
|---|---|---|
| ¿Se completó la tarea? | Tareas completadas / intentos | Escenario y criterio de éxito predefinidos |
| ¿Cuánto tardó? | Duración de la tarea | Mismo inicio/fin y condiciones comparables |
| ¿Cuántas veces necesitó ayuda? | Solicitudes de ayuda por tarea | Regla de observación estable |
| ¿Qué problema bloqueó a la persona? | Hallazgos y severidad | Registro reproducible y contexto |
| ¿Se comprende el mensaje? | Respuesta u observación cualitativa | Pregunta y tarea bien definidas |

Si una persona completa el recorrido después de recibir instrucciones adicionales que no estaban previstas, **no debemos clasificarlo automáticamente como éxito autónomo**.

### Ejemplo de un recorrido

```text
Start: User opens the registration page
Goal: User registers a pet and publishes a support request
Success: Both records are created without intervention
```

El fragmento es un **criterio conceptual de observación**, no una función implementada.

### ¿Qué ocurre si no se realizó la prueba?

Registra:

```text
Human evaluation: Pending
Automated checks: Available
Conclusion: Human usability not yet validated
```

No conviertas una sesión de Playwright en una supuesta evaluación humana. Un navegador automatizado verifica ciertas interacciones; **no observa comprensión, dudas o expectativas de una persona real**.

---

## 15. Rendimiento: distinguir latencia, volumen, carga y percentiles

El desempeño necesita definiciones precisas.

- **Latencia:** tiempo de una operación individual.
- **Volumen de datos:** cantidad y características de registros procesados.
- **Concurrencia:** número de operaciones que pueden ocurrir al mismo tiempo.
- **Throughput:** operaciones completadas por unidad de tiempo bajo condiciones definidas.
- **Mediana (p50):** valor central de las observaciones ordenadas.
- **Percentil 95 (p95):** referencia de una posición alta de la distribución; según el método empleado, aproximadamente el 95 % de los valores quedan en o por debajo de ella.

Una sola duración no describe la variabilidad de múltiples solicitudes.

### Ejemplo hipotético de cinco muestras

```text
samplesMs = [8, 9, 10, 11, 35]

medianMs = 10
```

El valor más alto muestra que algunas operaciones pueden tardar mucho más que la duración habitual. Por eso conviene mirar percentiles y distribución, no solo un promedio.

### Precaución con pocas muestras

Un percentil calculado con 25 observaciones es **una descripción de esa muestra**, no un modelo estadístico completo del rendimiento bajo producción. La definición del algoritmo de percentiles también importa al comparar herramientas.

**🧠 Idea mental**

```text
Misma operación + mismo dataset + mismo método
                    ↓
          muestras ordenadas
                    ↓
              p50 y p95
                    ↓
     interpretación con límites
```

---

## 16. Caso de estudio: cómo mide PetMatch su reporte

La prueba real está en [ReportQueryPerformanceTests.java](../../src/test/java/com/petmatch/community/integration/ReportQueryPerformanceTests.java).

La implementación crea registros sintéticos para la consulta administrativa:

**Código real — constantes:**

```java
private static final int ROWS = 120;
private static final int SAMPLES = 25;
```

Después prepara filtros por estado `OPEN` y tipo `WALK`, verifica el total y realiza llamadas de calentamiento.

**Código real — parte de la medición:**

```java
long[] durations = new long[SAMPLES];

for (int index = 0; index < SAMPLES; index++) {
    long started = System.nanoTime();
    var report = service.search(filter, 0);
    durations[index] = System.nanoTime() - started;

    assertEquals(ROWS, report.rows().getTotalElements());
    assertEquals(20, report.rows().getNumberOfElements());
}

Arrays.sort(durations);

double medianMs = durations[SAMPLES / 2] / 1_000_000.0;
double p95Ms =
        durations[(int) Math.ceil(SAMPLES * .95) - 1] / 1_000_000.0;
```

### ¿Qué ocurre aquí?

1. `System.nanoTime()` mide duración transcurrida, no una fecha de calendario.
2. La diferencia entre dos llamadas se conserva en nanosegundos.
3. Se verifica que el resultado sigue siendo correcto en cada medición.
4. `Arrays.sort` organiza las observaciones de menor a mayor.
5. Con 25 muestras, `durations[SAMPLES / 2]` corresponde al **decimotercer valor ordenado**, la mediana.
6. La fórmula de p95 usada en el código toma el **vigésimo cuarto valor ordenado**: índice `23` en un arreglo basado en cero.
7. Se convierte de nanosegundos a milisegundos.

**Importante:** el p95 del código utiliza la convención *nearest-rank*. Otras herramientas pueden interpolar percentiles y producir cifras ligeramente diferentes con los mismos datos.

### ¿Qué mide y qué no mide?

**Mide** el tiempo de la llamada a `service.search(filter, 0)` en el contexto de esa prueba. La operación incluye la lógica del Service y las consultas relacionadas al Repository.

**No mide** el tiempo completo del navegador, el transporte HTTP, la descarga de PDF ni toda la experiencia del usuario.

### Efectos del entorno

La prueba utiliza `@SpringBootTest` y `@Transactional`, crea datos en la base de pruebas y ejecuta varias consultas dentro de ese contexto. El motor de persistencia, su caché de primer nivel, el calentamiento de JVM y el entorno de CI pueden influir en los tiempos.

Eso significa que los números son útiles como **línea base técnica del escenario**, pero no pueden presentarse como una garantía de latencia productiva.

---

## 17. El archivo de medición y la trazabilidad de una ejecución

La prueba escribe:

```text
target/report-query-performance.json
```

Su estructura contiene campos como:

```json
{
  "scope": "service-level-report-query",
  "fixture_rows": 120,
  "warmups": 5,
  "samples": 25,
  "median_ms": 0.0,
  "p95_ms": 0.0,
  "commit": "local"
}
```

**El JSON anterior es un ejemplo de estructura, no un resultado de medición.** Los ceros son marcadores ilustrativos: no deben interpretarse ni publicarse como tiempos observados. En el archivo real los valores se calculan durante la ejecución.

El campo `commit` toma la variable de entorno `GITHUB_SHA` cuando está disponible. Si no lo está, el código escribe `local`. En ese segundo caso necesitas registrar manualmente la revisión del repositorio si quieres comparar el resultado con otra versión.

### Para comparar dos mediciones

Registra al menos:

- revisión del código;
- versión de Java, framework y base de datos;
- hardware o runner;
- volumen y distribución de datos;
- filtros utilizados;
- condiciones de concurrencia;
- número de muestras y calentamiento;
- definición de inicio y fin de la operación;
- algoritmo de cálculo de percentiles.

Un p95 menor puede deberse a una mejora del código, a una caché caliente o a un equipo más rápido. **Sin contexto, la diferencia no demuestra la causa**.

---

## 18. GitHub Actions: automatizar la recopilación sin confundirla con validación total

El repositorio contiene [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).

El workflow se ejecuta en eventos `push`, `pull_request` y `workflow_dispatch`. Prepara Java 21, un servicio MySQL de pruebas y ejecuta:

**Código real — comando de CI:**

```bash
./mvnw -B -ntp clean verify
```

También contiene:

```bash
python3 tools/check-accessibility.py
python3 -m py_compile tools/browser-smoke.py
```

### Qué hace cada paso

| Paso | Evidencia que aporta |
|---|---|
| `clean verify` | Compilación, tests y generación del reporte JaCoCo configurado |
| `check-accessibility.py` | Verificación estática de asociaciones de errores |
| `py_compile` | Verificación de sintaxis Python del script de navegador |
| `upload-artifact` | Conservación de ciertos archivos generados por el workflow |

La configuración publica como artefactos:

```text
target/surefire-reports/
target/site/jacoco/
target/report-query-performance.json
```

### Dos precauciones

**Primera:** un artefacto subido por CI refleja una ejecución. Verifica su estado, commit y contenidos antes de interpretarlo.

**Segunda:** `py_compile` **no ejecuta el navegador**. El script [tools/browser-smoke.py](../../tools/browser-smoke.py) utiliza Playwright, pero su recorrido completo no forma parte de los pasos descritos de este workflow.

**⚠️ Error frecuente:** escribir «prueba de navegador aprobada en CI» únicamente porque la sintaxis del script fue comprobada.

---

## 19. Cómo leer una evidencia técnica del repositorio

Para evaluar una versión, empieza por las fuentes y después formula conclusiones.

### Paso 1 — Identifica la revisión

Desde la raíz de una copia del repositorio:

```bash
git rev-parse HEAD
git status --short
```

El primer comando identifica el commit activo. El segundo permite detectar modificaciones locales no confirmadas.

### Paso 2 — Ejecuta la suite solo en una base aislada

PetMatch utiliza una configuración de pruebas con variables específicas:

```text
TEST_DB_URL
TEST_DB_USERNAME
TEST_DB_PASSWORD
```

La configuración se encuentra en [application-test.yaml](../../src/test/resources/application-test.yaml).

**No apuntes esas variables a producción ni a una base con datos que debas conservar.** El perfil de pruebas utiliza Hibernate con una configuración de esquema que puede modificar la base de tests.

Con un esquema **exclusivo y seguro** preparado, ejecuta:

```bash
./mvnw clean verify
```

### Paso 3 — Revisa los archivos generados

En una ejecución correctamente configurada, inspecciona:

```text
target/surefire-reports/TEST-*.xml
target/site/jacoco/index.html
target/site/jacoco/jacoco.xml
target/report-query-performance.json
```

Si falta alguno, **no sustituyas su ausencia por un valor cero**. Revisa si se ejecutó el plugin correspondiente, si falló el build o si la prueba se omitió.

### Paso 4 — Interpreta la evidencia

Escribe una conclusión por cada pregunta:

- ¿Las pruebas ejecutadas pasaron?
- ¿Qué cobertura de líneas registró JaCoCo?
- ¿Qué casos de negocio críticos están cubiertos?
- ¿Qué operación representa la medición de p95?
- ¿Qué verificaciones no se realizaron?

### Paso 5 — Separa pendientes

Una ejecución automatizada puede completarse sin que exista todavía una evaluación humana de usabilidad o una revisión de accesibilidad integral.

Anota esas actividades como **pendientes**, sin desvalorizar ni exagerar lo que sí se comprobó.

---

## 20. El recopilador histórico: una advertencia de mantenimiento importante

PetMatch conserva [tools/collect-quality-evidence.py](../../tools/collect-quality-evidence.py).

Este script:

1. lee XML de Surefire;
2. abre `target/site/jacoco/jacoco.xml`;
3. calcula totales, cobertura global y resultados por clases seleccionadas;
4. obtiene el commit mediante Git;
5. copia XML y genera huellas SHA-256 de fuentes;
6. escribe un `metrics.json`.

**Código real — parte del cálculo de cobertura:**

```python
def lines(node):
    c = next(
        c for c in node.findall('counter')
        if c.get('type') == 'LINE'
    )
    covered = int(c.get('covered'))
    missed = int(c.get('missed'))
    return {
        'covered': covered,
        'missed': missed,
        'percent': (
            round(100 * covered / (covered + missed), 2)
            if covered + missed else None
        )
    }
```

### Un límite real del código actual

El script todavía utiliza como **destino de salida**:

```text
docs/08-aplicacion-cronograma/evidencias/
```

Esa ruta pertenece al antiguo material asociado a una intervención concreta. Ejecutar el script puede **sobrescribir archivos de evidencia de esa carpeta**. Por eso no conviene utilizarlo sin antes definir un destino nuevo para ejecuciones actuales o conservar una copia verificable de los registros históricos.

Además, la lista de clases seleccionadas del script incluye nombres de dos estrategias antiguas de agrupación SQL que fueron eliminadas. En una ejecución nueva esas clases pueden no aparecer; el script no debe inducir a interpretar la ausencia como una cobertura actual de esas implementaciones.

**Decisión editorial:** este capítulo explica el mecanismo de recolección, pero **no modifica el script ni regenera sus salidas históricas**.

### Cómo debería evolucionar la recopilación

Una mejora futura podría separar claramente:

```text
quality-records/
    archived/
        historical-run/
    current/
        run-identified-by-commit/
```

Esta estructura es una **alternativa propuesta**, no una carpeta que afirmemos implementada.

La regla importante es no mezclar **material formativo estable** con **resultados temporales de una ejecución**.

---

## 21. Una matriz de indicadores pequeña y defendible

El objetivo no es crear una hoja con decenas de porcentajes, sino conservar una relación clara entre pregunta, fuente y decisión.

### Plantilla conceptual

| Indicador | Pregunta | Fuente | Regla de interpretación |
|---|---|---|---|
| Pruebas aprobadas | ¿Pasaron los casos ejecutados? | Surefire XML | Informar totales, fallos, errores y omitidas |
| Cobertura de líneas | ¿Qué líneas ejecutó la suite instrumentada? | JaCoCo XML | Informar cubiertas, omitidas y alcance |
| Criterios críticos verificados | ¿Qué riesgos tienen pruebas adecuadas? | Matriz requisito ↔ prueba | Revisar contenido real de cada prueba |
| Defectos abiertos | ¿Qué hallazgos siguen sin resolverse? | Registro de hallazgos | Informar severidad y edad/contexto |
| Asociación técnica de errores | ¿Los campos revisados enlazan mensajes? | Script estático | Declarar plantillas y campos revisados |
| Éxito de tareas humanas | ¿Pueden las personas completar el flujo? | Observación de usuarios | Marcar pendiente si no se ejecutó |
| Latencia mediana y p95 | ¿Cómo se comporta una operación medida? | JSON de medición | Registrar datos y método de ejecución |

### Ejemplo de estado sin inventar números

```text
Security regression checks: Available in repository
Human usability validation: Pending
Performance measurement method: Implemented
Current measured values: Read from the identified execution artifact
```

En un informe real, «disponible en el repositorio» y «ejecutado satisfactoriamente» **no son el mismo estado**.

### ¿Cuándo una métrica se vuelve útil?

Cuando conduce a una decisión:

```text
Observation
    ↓
Interpretation
    ↓
Decision
    ↓
Verification after the change
```

Si nadie sabe qué decisión tomaría al observar un indicador, conviene cuestionar si es necesario recolectarlo.

---

## 22. Interpretar cambios: antes y después necesitan condiciones comparables

Imagina que una ejecución informa p95 mayor que otra. No debes concluir inmediatamente que el último cambio degradó el rendimiento.

Primero verifica:

1. ¿Se midió el mismo método?
2. ¿El conjunto de datos tiene la misma distribución?
3. ¿El motor de base de datos y sus índices son comparables?
4. ¿El hardware es equivalente?
5. ¿La carga concurrente fue la misma?
6. ¿El cálculo del percentil utiliza la misma definición?
7. ¿Hubo cambios en la caché o en el calentamiento?
8. ¿Las dos ejecuciones están identificadas por commit?

### Dos comparaciones legítimas

**Comparación de funcionalidad:** antes fallaba un caso de frontera; después la misma prueba pasa. Esta evidencia indica una corrección del comportamiento comprobado.

**Comparación de rendimiento:** se repite un experimento con condiciones controladas, suficientes observaciones y una diferencia interpretada según la variabilidad. Esta evidencia permite argumentar una mejora del escenario medido.

### Una comparación inválida

```text
Before: 59.2 % coverage, historical commit
After: 100 % tests passed, different commit
Conclusion: Quality increased by 40.8 points
```

Se están restando **métricas diferentes** con **denominadores diferentes**. La conclusión no tiene significado técnico.

**🧠 Idea mental:** antes de comparar dos números, comprueba que miden **la misma propiedad** bajo condiciones suficientemente equivalentes.

---

## 23. ⚠️ Errores frecuentes

### Error A — Confundir porcentaje de tests aprobados con cobertura

**Incorrecto:** «49 pruebas aprobaron, entonces la cobertura es 100 %».

**Corrección:** Surefire y JaCoCo miden cosas distintas. Consulta cada reporte.

### Error B — Omitir pruebas ignoradas del resultado

**Incorrecto:** «Todo pasó» sin indicar que varias pruebas fueron omitidas.

**Corrección:** informa los cuatro conteos: tests, failures, errors y skipped.

### Error C — Dividir por cero y reemplazar el resultado arbitrariamente

**Incorrecto:** definir 100 % de cumplimiento porque todavía no hay criterios medidos.

**Corrección:** marca la métrica como **no aplicable** o **pendiente**, según corresponda.

### Error D — Tratar la cobertura como una medida de ausencia de defectos

**Incorrecto:** «JaCoCo muestra 90 %, por tanto el módulo es seguro».

**Corrección:** diseña casos de riesgo reales, especialmente autorizaciones y transiciones de estado.

### Error E — Presentar un test estático como certificación WCAG

**Incorrecto:** «14 campos con asociación correcta = 100 % accesible».

**Corrección:** limita la afirmación a las plantillas y reglas realmente verificadas.

### Error F — Atribuir a producción una medida de CI

**Incorrecto:** «El reporte siempre responde en p95 de 18 ms».

**Corrección:** identifica el método y los datos del test; mide escenarios de producción por separado si son necesarios.

### Error G — Inventar resultados de entrevistas o validación

**Incorrecto:** registrar tres recorridos humanos como exitosos sin participantes ni observación.

**Corrección:** conserva el estado **pendiente de evaluación humana**.

### Error H — Mezclar registros de distintos commits

**Incorrecto:** reutilizar una cobertura antigua para explicar clases que ya no existen.

**Corrección:** cada resultado debe conservar su revisión y método; no mezcles versiones sin advertirlo.

### Error I — Sobrescribir registros históricos

**Incorrecto:** ejecutar un recopilador que escribe sobre una evidencia anterior sin controlar el destino.

**Corrección:** separa las ejecuciones y conserva la trazabilidad del registro original.

### Error J — Usar métricas para justificar patrones artificiales

**Incorrecto:** agregar interfaces innecesarias para aumentar el número de clases «bien diseñadas».

**Corrección:** la mantenibilidad requiere decisiones proporcionadas y comprobables, no contar abstracciones.

---

## 24. 🔁 Llévalo a tu proyecto

No copies las clases \`SupportRequest\`, los filtros de mascotas ni el conjunto de métricas de PetMatch. Selecciona **un riesgo verdadero** de tu aplicación y define cómo lo comprobarás.

### Paso 1 — Describe el producto y una necesidad

Ejemplo de otro dominio: un sistema de biblioteca debe impedir que un usuario devuelva un libro que no tiene prestado.

### Paso 2 — Identifica una característica de calidad

Relaciona la necesidad con el modelo ISO/IEC 25010:2023. Para el ejemplo, el comportamiento correcto del préstamo se relaciona principalmente con adecuación funcional y confiabilidad.

### Paso 3 — Define un criterio observable

Escribe una situación reproducible y un resultado esperado:

> «Al intentar devolver un préstamo inexistente, la operación se rechaza y el estado del libro permanece sin modificar».

### Paso 4 — Elige un nivel de prueba

Decide si basta con una prueba unitaria de Service o si también necesitas integración con base de datos o HTTP. Justifica la frontera.

### Paso 5 — Ejecuta una medición real

Utiliza los reportes del stack de tu proyecto, como JUnit/Surefire y JaCoCo si están configurados. No agregues una herramienta solamente para obtener más gráficos.

### Paso 6 — Registra fuente y denominador

Conserva los valores originales y su definición. Si calculas un porcentaje, indica qué entra en numerador y denominador.

### Paso 7 — Registra el contexto

Guarda commit, entorno, versión del lenguaje, base de datos y condiciones necesarias para repetir la ejecución.

### Paso 8 — Separa lo verificado de lo pendiente

Si tu aplicación tiene una pantalla que debe evaluar otra persona, no conviertas una prueba HTTP en aceptación humana.

### Paso 9 — Interpreta y decide

Explica qué acción tomarías si el criterio no se cumple. Una métrica sin interpretación no es todavía un instrumento útil de mejora.

### Matriz mínima de entrega

| Necesidad | Riesgo | Criterio | Evidencia | Resultado | Pendiente |
|---|---|---|---|---|---|
| Por completar | Por completar | Por completar | Por completar | Sin inventar | Si aplica |

La práctica se completa con **criterios claros y evidencia auténtica**. No necesitas producir un informe PDF, un dashboard ni una hoja de cálculo compleja para demostrar comprensión.

---

## 25. 🧪 Comprueba que entendiste

Responde sin consultar las fórmulas del capítulo:

1. ¿Por qué una métrica debe responder primero a una pregunta?
2. ¿Qué diferencia existe entre característica de calidad, requisito y criterio de aceptación?
3. ¿Cuál es la diferencia entre una meta y una línea base?
4. ¿Qué cuatro conteos necesitas para interpretar una suite de Surefire?
5. ¿Cómo se calcula la cantidad de pruebas aprobadas?
6. ¿Por qué el porcentaje sobre pruebas ejecutadas puede diferir del porcentaje sobre todas las reportadas?
7. ¿Qué significa que una prueba esté omitida?
8. ¿Qué mide la cobertura de líneas de JaCoCo?
9. ¿Qué significa un contador XML \`LINE\` con `covered` y `missed`?
10. ¿Por qué cobertura de líneas y cobertura de ramas no equivalen?
11. ¿Por qué una cobertura alta no demuestra ausencia de defectos?
12. ¿Qué diferencia existe entre contar pruebas y comprobar requisitos críticos?
13. ¿Qué se entiende por trazabilidad requisito ↔ prueba?
14. ¿Por qué no puedes comparar un porcentaje de cobertura con un porcentaje de tests aprobados?
15. ¿Qué características hacen reproducible una medición de rendimiento?
16. ¿Cuántos registros, calentamientos y muestras utiliza el test de reportes de PetMatch?
17. ¿Qué operación mide `ReportQueryPerformanceTests`?
18. ¿Por qué una medición con `@Transactional` y datos sintéticos no describe automáticamente producción?
19. ¿Qué significa p95 y qué posición usa el código con 25 muestras?
20. ¿Qué comprueba `tools/check-accessibility.py`?
21. ¿Por qué `py_compile` no demuestra que Playwright ejecutó un recorrido?
22. ¿Qué riesgo presenta el destino actual de `tools/collect-quality-evidence.py`?
23. ¿Por qué no conviene reutilizar el porcentaje histórico de octubre como cobertura actual?
24. ¿Qué debes registrar si no has realizado una evaluación humana?
25. ¿Cómo convertirías una medición en una decisión de mejora concreta?

### Diagnóstico — interpretar sin inventar

Un equipo presenta estos datos **hipotéticos**:

```text
Reported tests: 30
Failures: 1
Errors: 0
Skipped: 4

JaCoCo covered lines: 180
JaCoCo missed lines: 60

Measured samples: 25
Human usability sessions: Not performed
```

**Actividad:**

1. Calcula las pruebas aprobadas.
2. Calcula la tasa de aprobación sobre todas las pruebas reportadas.
3. Calcula la tasa de aprobación sobre las realmente ejecutadas.
4. Calcula la cobertura de líneas.
5. Explica por qué el defecto de la suite no desaparece al calcular un porcentaje.
6. Explica por qué ninguna de esas cifras demuestra conformidad WCAG.
7. Decide qué información falta para interpretar la latencia de las 25 muestras.
8. Redacta una conclusión breve que separe **resultados técnicos** de **validación humana pendiente**.

Después identifica en PetMatch el archivo de Surefire, JaCoCo o el test que correspondería a cada dato.

---

## ✅ Qué debes recordar

**Una cifra es útil cuando sabemos qué representa, de dónde proviene, cómo se calculó y qué decisión permite tomar.**

En PetMatch, Surefire describe la ejecución de pruebas; JaCoCo describe partes del código instrumentado; las pruebas de integración aportan evidencia de comportamientos concretos; la medición de reportes describe un escenario técnico; y las comprobaciones estáticas de accesibilidad cubren un subconjunto limitado de reglas.

Ninguna de esas evidencias sustituye automáticamente a las otras. Tampoco certifica por sí sola el cumplimiento de todo un modelo de calidad.

El método transferible es:

**necesidad → riesgo → criterio → método → medición → interpretación → decisión**.

Y siempre con **versión, condiciones y límites explícitos**.

### Para continuar

- [31 — Pruebas unitarias](../06-calidad-y-recorrido/31-pruebas-unitarias.md).
- [32 — Pruebas de integración](../06-calidad-y-recorrido/32-pruebas-de-integracion.md).
- [34 — Buenas prácticas y decisiones](../06-calidad-y-recorrido/34-buenas-practicas-y-decisiones.md).
- [41 — Reportes dinámicos con JPA](41-reportes-dinamicos-jpa.md).
- [43 — Usabilidad, navegación y accesibilidad](43-usabilidad-navegacion-y-accesibilidad.md).
- [44 — Modelos de calidad del software](44-modelos-de-calidad-del-software.md).

El **capítulo 46** profundizará en verificación y validación, para distinguir qué comprueban las pruebas técnicas y qué significa realmente confirmar una necesidad con las personas interesadas.

### Referencias oficiales

- [ISO/IEC 25010:2023 — Modelo de calidad del producto](https://www.iso.org/standard/78176.html).
- [Maven Surefire — Reportes de pruebas](https://maven.apache.org/surefire/maven-surefire-plugin/).
- [JaCoCo — Documentación](https://www.jacoco.org/jacoco/trunk/doc/).
- [JaCoCo — Formato XML y contadores](https://github.com/jacoco/jacoco/blob/master/org.jacoco.report/src/org/jacoco/report/xml/report.dtd).
- [GitHub Actions — Almacenar y compartir artefactos](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts).
- [W3C WAI — Evaluar la accesibilidad web](https://www.w3.org/WAI/test-evaluate/).

Este capítulo utiliza archivos concretos de PetMatch como ejemplo, pero **no recalcula métricas de la rama ni altera evidencias anteriores**. Las fórmulas y fragmentos hipotéticos se distinguen de la implementación y de los resultados observados.
