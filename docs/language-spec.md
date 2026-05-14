# GymScript: vision general

GymScript es un DSL tematico para describir metas, repeticiones y rutinas de entrenamiento. La sintaxis prioriza palabras de gimnasio sobre simbolos genericos.

## Pipeline

1. Analisis lexico
2. Analisis sintactico
3. Analisis semantico
4. Evaluacion

## Construcciones implementadas

- Variables con `peso`
- Reasignacion con `cargar`
- Impresion con `mostrar`
- Condicional `si_fuerza`
- Rama alternativa `descanso`
- Ciclo `mientras_entrenas`
- Rutinas `rutina` y llamadas `llamar`
- Listas con `lista`, `tomar` y `largo`

## Tipos de valor

- Numero
- String
- Booleano
- Lista
- Rutina
- Null

## Reglas semanticas

- Una variable debe declararse antes de usarse.
- No se puede redeclarar una variable en el mismo alcance.
- Una rutina debe existir antes de invocarse.
- La aridad de una rutina debe coincidir con los argumentos recibidos.
- Las condiciones de `si_fuerza` y `mientras_entrenas` deben ser booleanas.
- `tomar` y `largo` operan solo sobre listas.

## Compatibilidad legacy

Se mantiene compatibilidad temporal con estos simbolos para no romper migraciones existentes:

- `+ - * /`
- `= == != > < >= <=`
- `(` `)` `,`

La sintaxis recomendada y documentada es exclusivamente la tematica.
