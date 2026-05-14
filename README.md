# GymScript

GymScript es un mini-lenguaje académico inspirado en gimnasio, rutinas, ejercicios y repeticiones. Este repositorio contiene la estructura base para construir un intérprete completo en Scala con etapas separadas de análisis léxico, análisis sintáctico, análisis semántico e interpretación.

## Estado actual

La primera fase deja preparado el proyecto para crecer de forma ordenada:

- `lexer`: tokenización y reporte de errores léxicos.
- `parser`: contrato y AST iniciales.
- `interpreter`: valores, entorno y ejecución base de nodos del AST.
- `resolver`: espacio para validaciones semánticas.
- `docs` y `examples`: especificación y programas de referencia.
- `src/test`: pruebas automatizadas iniciales con ScalaTest.

## Requisitos

- Java 17 o superior recomendado.
- `sbt` instalado en el sistema.

## Cómo ejecutar

```bash
sbt "run examples/basic-routine.gym.txt"
```

En esta fase el flujo CLI ya carga el archivo, ejecuta el lexer y conecta parser e intérprete con una base extensible. El parser todavía está en modo esqueleto, por lo que la ejecución completa del lenguaje se implementará en la siguiente fase.

## Cómo correr pruebas

```bash
sbt test
```

## Estructura

```text
examples/   Programas GymScript de referencia
docs/       Especificación inicial del lenguaje
src/main/   Implementación del intérprete
src/test/   Pruebas automatizadas
```

## Próximos incrementos recomendados

1. Completar el lexer con más casos límite y recuperación de errores.
2. Implementar parser descendente recursivo con precedencia de operadores.
3. Añadir análisis semántico para variables y alcance.
4. Conectar parser e intérprete para ejecutar ejemplos end-to-end.

