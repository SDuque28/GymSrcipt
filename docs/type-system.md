# Sistema de tipos

GymScript implementa un sistema semantico minimo:

- `numero`
- `texto`
- `booleano`
- `sin_resultado`
- `lista_de <tipo>`
- `desconocido` como tipo interno de recuperacion

## Reglas

- Literales numericos producen `numero`.
- Strings producen `texto`.
- `verdadero` y `falso` producen `booleano`.
- `sin_resultado` produce `sin_resultado`.
- `lista` infiere `lista_de <tipo>` si todos los elementos son compatibles.
- `tomar(lista, indice)` retorna el tipo del elemento.
- `largo(lista)` retorna `numero`.
- `rango_set(lista, inicio, fin)` retorna `lista_de <tipo>`.

## Rutinas

Sintaxis recomendada:

```text
rutina saludar abre_set nombre como texto cierra_set entrega texto inicio_rutina
  entregar_resultado "Hola " mas_reps nombre
fin_rutina
```

## Validaciones

- Los argumentos deben coincidir con el tipo del parametro.
- Los retornos deben coincidir con `entrega`.
- `si_fuerza` y `mientras_entrenas` requieren `booleano`.
- Las listas son homogeneas.
