# GymScript

GymScript es un mini-lenguaje academico inspirado en gimnasio, rutinas, ejercicios y repeticiones. El proyecto implementa un flujo completo en Scala:

`source .txt -> lexer -> parser -> analisis semantico -> interprete -> salida`

## Estado implementado

- Lexer con acumulacion de errores, posiciones y soporte para strings, numeros, comentarios `#`, operadores y keywords.
- Parser descendente recursivo con precedencia de operadores y recuperacion basica de errores.
- Analisis semantico con alcance por bloque.
- Interprete con variables, reasignacion, `mostrar`, `si_fuerza`, `descanso`, `mientras_entrenas` y expresiones.
- Pruebas unitarias y prueba end-to-end con ScalaTest.

## Requisitos

- Java 17 o superior.
- `sbt`.

## Como ejecutar

```bash
sbt "run examples/basic-routine.gym.txt"
sbt "run examples/exhaustive-routine.gym.txt"
```

## Como correr validaciones

```bash
sbt compile
sbt test
```

## Reglas principales del lenguaje

- Declaracion: `peso nombre = expresion`
- Reasignacion: `nombre = expresion`
- Impresion: `mostrar(expresion)`
- Condicional:

```text
si_fuerza condicion
  ...
descanso
  ...
fin_rutina
```

- Ciclo:

```text
mientras_entrenas condicion
  ...
fin_rutina
```

## Alcance

GymScript usa alcance por bloque:

- El scope global contiene las variables top-level.
- Cada bloque de `si_fuerza`, `descanso`, `mientras_entrenas` y `{ ... }` abre un scope hijo.
- Se permite reasignar variables ya declaradas en scopes visibles.
- No se permite redeclarar una variable en el mismo scope.

## Estructura

```text
examples/   Programas GymScript ejecutables
docs/       Especificacion sincronizada con la implementacion
src/main/   Lexer, parser, analisis semantico, interprete y CLI
src/test/   Pruebas unitarias e integracion
```
