# GymScript

GymScript es un mini-lenguaje academico inspirado en gimnasio, rutinas y repeticiones. El proyecto implementa el flujo completo:

`source -> lexer -> parser -> resolucion de modulos -> analisis semantico -> interprete -> salida`

## Capacidades actuales

- Sintaxis tematica estricta por defecto.
- Modo `legacy` opcional desde CLI.
- Rutinas con parametros tipados, retorno tipado e inferencia controlada.
- `llamar` como expresion o sentencia.
- Listas homogeneas con acceso, mutacion, agregado, remocion y slicing.
- Imports seguros con `importar_rutina`.
- Tail recursion simple optimizada cuando la llamada recursiva queda en `entregar_resultado`.
- CLI con `--legacy`, `--strict`, `--tokens`, `--ast` y `--help`.

## Ejecucion

```bash
sbt "run examples/basic-routine.gym.txt"
sbt "run examples/exhaustive-routine.gym.txt"
sbt "run examples/advanced-routine.gym.txt"
sbt "run examples/modular/main.gym"
sbt "run examples/tail-recursion.gym"
```

## Validacion

```bash
sbt clean compile
sbt test
```

## CLI

```bash
sbt "run <archivo.gym|archivo.gym.txt> [--strict] [--legacy] [--tokens] [--ast]"
```

- `--strict`: fuerza el modo tematico estricto. Es el valor por defecto.
- `--legacy`: habilita compatibilidad con `+ - * / = == != > < >= <= ( ) , { }`.
- `--tokens`: imprime los tokens del archivo principal.
- `--ast`: imprime el AST resuelto, incluyendo rutinas importadas.
- `--help`: muestra la ayuda.

## Sintaxis base

- Declaracion: `peso nombre cargar expresion`
- Asignacion: `nombre cargar expresion`
- Salida: `mostrar abre_set expresion cierra_set`
- Retorno: `entregar_resultado expresion`
- Incremento: `subir_peso variable [por expresion]`
- Decremento: `bajar_peso variable [por expresion]`
- Import: `importar_rutina "math.gym"`

## Tipos soportados

- `numero`
- `texto`
- `booleano`
- `lista_de <tipo>`
- `sin_resultado`

## Ejemplo de rutina tipada

```text
rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
  entregar_resultado a mas_reps b
fin_rutina
```

## Documentacion adicional

- `docs/language-spec.md`
- `docs/type-system.md`
- `docs/modules-and-imports.md`
- `docs/error-handling.md`
- `docs/production-readiness.md`
