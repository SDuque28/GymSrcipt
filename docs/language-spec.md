# GymScript: vision general

GymScript es un mini-lenguaje tematico para modelar rutinas, metas y decisiones de entrenamiento. La implementacion actual sigue una arquitectura clasica de interprete con cuatro etapas:

1. Analisis lexico
2. Analisis sintactico
3. Analisis semantico
4. Evaluacion

## Construcciones implementadas

- Declaracion de variable con `peso`
- Reasignacion
- Impresion con `mostrar(...)`
- Condicional `si_fuerza ... descanso ... fin_rutina`
- Condicional sin `descanso`
- Ciclo `mientras_entrenas ... fin_rutina`
- Expresiones numericas, booleanas y de texto
- Agrupacion con parentesis

## Tipos de valor

- Numero
- String
- Booleano
- Null

## Reglas semanticas activas

- Una variable debe declararse antes de usarse.
- No se puede redeclarar una variable en el mismo scope.
- La reasignacion exige que la variable ya exista.
- Las condiciones de `si_fuerza` y `mientras_entrenas` deben ser booleanas.
- Los operadores aritmeticos y logicos validan tipos compatibles.

## Convenciones

- Comentarios de linea con `#`.
- Los saltos de linea separan sentencias.
- Los errores reportan linea, columna e indice absoluto.
- El lenguaje usa alcance por bloque.
