# GymScript

GymScript es un mini-lenguaje academico inspirado en gimnasio, rutinas y repeticiones. La implementacion actual ya cubre el flujo completo:

`source .txt -> lexer -> parser -> analisis semantico -> interprete -> salida`

## Estado final implementado

- Sintaxis tematica estricta por defecto.
- Modo `legacy` opcional para operadores y delimitadores antiguos.
- Rutinas con parametros, llamadas y `entregar_resultado`.
- `llamar` como expresion o como sentencia.
- Listas homogeneas con acceso, largo, mutacion, agregar, quitar y slicing.
- `subir_peso` y `bajar_peso` con cantidad opcional.
- CLI con `--legacy`, `--strict`, `--tokens`, `--ast` y `--help`.
- Errores uniformes: lexical, parse, semantic y runtime.

## Ejecucion

```bash
sbt "run examples/basic-routine.gym.txt"
sbt "run examples/exhaustive-routine.gym.txt"
sbt "run examples/advanced-routine.gym.txt"
```

## CLI

```bash
sbt "run <archivo.gym.txt> [--strict] [--legacy] [--tokens] [--ast]"
```

- `--strict`: modo tematico estricto. Es el default.
- `--legacy`: habilita compatibilidad parcial con `+ - * / = == != > < >= <= ( ) ,`.
- `--tokens`: imprime la secuencia de tokens.
- `--ast`: imprime el AST generado.
- `--help`: muestra la ayuda.

## Reglas principales

- Declaracion: `peso nombre cargar expresion`
- Asignacion: `nombre cargar expresion`
- Salida: `mostrar abre_set expresion cierra_set`
- Return: `entregar_resultado expresion`
- Incremento: `subir_peso variable [por expresion]`
- Decremento: `bajar_peso variable [por expresion]`

## Operaciones de lista

- Crear: `lista abre_set ... cierra_set`
- Tomar: `tomar abre_set lista separa indice cierra_set`
- Largo: `largo abre_set lista cierra_set`
- Cambiar: `cambiar_set abre_set lista separa indice separa valor cierra_set`
- Agregar: `agregar_set abre_set lista separa valor cierra_set`
- Quitar: `quitar_set abre_set lista separa indice cierra_set`
- Rango: `rango_set abre_set lista separa inicio separa fin cierra_set`

## Validacion

```bash
sbt clean compile
sbt test
```
