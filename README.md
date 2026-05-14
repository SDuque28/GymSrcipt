# GymScript

GymScript es un mini-lenguaje academico inspirado en gimnasio, rutinas, ejercicios y repeticiones. En esta fase ya funciona de punta a punta con una sintaxis tematica orientada a entrenamiento:

`source .txt -> lexer -> parser -> analisis semantico -> interprete -> salida`

## Estado implementado

- Lexer con operadores y delimitadores tematicos.
- Parser descendente recursivo con precedencia.
- Bloques estrictos con `inicio_rutina` y `fin_rutina`.
- Analisis semantico con alcance por bloque.
- Rutinas simples con parametros y llamadas.
- Listas simples con `lista`, `tomar` y `largo`.
- Compatibilidad temporal con parte de la sintaxis legacy: `+ - * / = == != > < >= <= ( ) ,`.

## Sintaxis principal

- Declaracion: `peso nombre cargar expresion`
- Reasignacion: `nombre cargar expresion`
- Impresion: `mostrar abre_set expresion cierra_set`
- Condicional:

```text
si_fuerza condicion inicio_rutina
  ...
descanso inicio_rutina
  ...
fin_rutina
```

- Ciclo:

```text
mientras_entrenas condicion inicio_rutina
  ...
fin_rutina
```

- Rutina:

```text
rutina nombre abre_set parametro1 separa parametro2 cierra_set inicio_rutina
  ...
fin_rutina
```

- Llamada:

```text
llamar nombre abre_set valor1 separa valor2 cierra_set
```

## Operadores tematicos

| Operacion | Sintaxis |
| --- | --- |
| suma | `mas_reps` |
| resta | `menos_reps` |
| multiplicacion | `series_de` |
| division | `dividir_rutina` |
| asignacion | `cargar` |
| mayor que | `levanta_mas_que` |
| menor que | `levanta_menos_que` |
| igual | `levanta_igual_que` |
| diferente | `no_levanta_igual` |
| mayor o igual | `levanta_minimo` |
| menor o igual | `levanta_maximo` |
| and | `y_entrena` |
| or | `o_descansa` |
| not | `sin_energia` |

## Delimitadores tematicos

- `abre_set`
- `cierra_set`
- `separa`
- `inicio_rutina`
- `fin_rutina`

## Alcance

GymScript usa alcance por bloque:

- Scope global para declaraciones top-level.
- Scope hijo para `si_fuerza`, `descanso`, `mientras_entrenas` y `rutina`.
- Los parametros de rutina viven solo dentro de la rutina.

## Como ejecutar

```bash
sbt "run examples/basic-routine.gym.txt"
sbt "run examples/exhaustive-routine.gym.txt"
sbt "run examples/advanced-routine.gym.txt"
```

## Como validar

```bash
sbt compile
sbt test
```

## Ejemplos incluidos

- `examples/basic-routine.gym.txt`
- `examples/exhaustive-routine.gym.txt`
- `examples/advanced-routine.gym.txt`
