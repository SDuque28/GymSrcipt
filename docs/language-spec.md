# GymScript: visión general

GymScript es un mini-lenguaje temático para modelar rutinas, metas y decisiones de entrenamiento. Su diseño busca ser legible para estudiantes y lo bastante riguroso para implementar un pipeline clásico de intérprete.

## Objetivos

- Declarar variables con sintaxis inspirada en el gimnasio.
- Evaluar expresiones numéricas, booleanas y de texto.
- Ejecutar condicionales y ciclos.
- Producir salidas verificables desde pruebas automatizadas.

## Palabras clave iniciales

- `peso`: declaración de variable.
- `mostrar(...)`: impresión.
- `si_fuerza`: condicional.
- `descanso`: rama alternativa.
- `mientras_entrenas`: ciclo while.
- `fin_rutina`: cierre de bloque.
- `verdadero`, `falso`: booleanos.

## Convenciones iniciales

- Comentarios de línea con `#`.
- Una línea puede contener una instrucción principal.
- Los errores deben reportar línea, columna e índice absoluto.
- La semántica final debe ser determinista y fácil de probar.

