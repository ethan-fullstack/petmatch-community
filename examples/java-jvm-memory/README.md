# Laboratorio Java 21: referencias y memoria

Problema: modificar un objeto y reasignar una variable no son la misma operación. [MemoryDemo.java](MemoryDemo.java) pasa una copia del valor de referencia; la primera escritura llega al objeto compartido y la segunda solo cambia la variable local.

```bash
mkdir -p /tmp/petmatch-jvm
javac -d /tmp/petmatch-jvm examples/java-jvm-memory/MemoryDemo.java
java -cp /tmp/petmatch-jvm MemoryDemo
```

Salida comprobada:

```text
Dentro: Otra
Fuera: Sol
```

JDK: herramientas de desarrollo, incluido javac y runtime. JRE: concepto de entorno de ejecución (no todos los proveedores distribuyen un JRE separado). JVM: máquina que ejecuta bytecode y gestiona memoria. Modelo pedagógico: cada invocación tiene variables locales en un marco de pila; las instancias están en heap y las referencias permiten alcanzarlas. El JIT puede optimizar/inlining/eliminar asignaciones: no se afirma una topología física universal. Java siempre pasa por valor, también cuando ese valor representa una referencia.

Preguntas: ¿qué imprimirá si se invierten las dos líneas de change? ¿Se conserva una referencia a “Otra” fuera del método? ¿Cuándo resulta elegible para GC y por qué no se puede prometer el momento de recolección? Transfiere el razonamiento a un DTO propio sin modificar el dominio PetMatch.
