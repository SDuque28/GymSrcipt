# Especificacion de GymScript

GymScript modela programas como rutinas de entrenamiento. La sintaxis evita simbolos genericos en favor de verbos y palabras del dominio gimnasio.

## Palabras clave principales

- Variables: `peso`
- Salida: `mostrar`
- Condicional: `si_fuerza`, `descanso`
- Ciclo: `mientras_entrenas`
- Bloques: `inicio_rutina`, `fin_rutina`
- Rutinas: `rutina`, `llamar`, `entregar_resultado`
- Imports: `importar_rutina`
- Tipos: `como`, `entrega`, `numero`, `texto`, `booleano`, `lista_de`, `sin_resultado`
- Listas: `lista`, `tomar`, `largo`, `cambiar_set`, `agregar_set`, `quitar_set`, `rango_set`

## Operadores tematicos

- `mas_reps`
- `menos_reps`
- `series_de`
- `dividir_rutina`
- `cargar`
- `levanta_mas_que`
- `levanta_menos_que`
- `levanta_igual_que`
- `no_levanta_igual`
- `levanta_minimo`
- `levanta_maximo`
- `y_entrena`
- `o_descansa`
- `sin_energia`

## Delimitadores tematicos

- `abre_set`
- `cierra_set`
- `separa`

## Principios semanticos

- Las variables deben declararse antes de usarse.
- Las rutinas pueden declararse en el nivel superior y ser importadas desde otros archivos.
- Las condiciones de `si_fuerza` y `mientras_entrenas` deben ser booleanas.
- Las listas son homogeneas.
- El modo estricto es el modo por defecto.
