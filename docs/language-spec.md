# GymScript: especificacion final

GymScript es un DSL tematico para expresar rutinas, metas y transformaciones de entrenamiento.

## Caracteristicas implementadas

- Variables y expresiones
- Condicionales y ciclos
- Rutinas con parametros
- Retorno temprano con `entregar_resultado`
- Llamadas como expresion
- Listas homogeneas
- Mutaciones seguras de listas
- Ajustes numericos con `subir_peso` y `bajar_peso`

## Tipos de valor

- `numero`
- `texto`
- `booleano`
- `lista`
- `rutina`
- `nulo`

## Alcance

- Scope global para declaraciones top-level.
- Scope por bloque en `si_fuerza`, `descanso` y `mientras_entrenas`.
- Scope local por rutina.
- Los parametros viven solo dentro de su rutina.

## Legacy

GymScript corre en modo estricto por defecto. El modo `legacy` es opcional y solo habilita compatibilidad con simbolos y aliases anteriores.
