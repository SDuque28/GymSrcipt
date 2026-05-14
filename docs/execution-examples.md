# Ejemplos de ejecucion

## Rutina basica

```gymscript
peso repeticiones cargar 0
peso meta cargar 3

mientras_entrenas repeticiones levanta_menos_que meta inicio_rutina
  mostrar abre_set repeticiones cierra_set
  repeticiones cargar repeticiones mas_reps 1
fin_rutina
```

Salida:

```text
0
1
2
```

## Condicional tematico

```gymscript
si_fuerza verdadero y_entrena sin_energia falso inicio_rutina
  mostrar abre_set "Condicion valida" cierra_set
fin_rutina
```

## Rutina y llamada

```gymscript
rutina saludar abre_set nombre cierra_set inicio_rutina
  mostrar abre_set "Hola " mas_reps nombre cierra_set
fin_rutina

llamar saludar abre_set "Coach" cierra_set
```

## Listas

```gymscript
peso ejercicios cargar lista abre_set "curl" separa "sentadilla" separa "press" cierra_set
mostrar abre_set ejercicios cierra_set
mostrar abre_set tomar abre_set ejercicios separa 1 cierra_set cierra_set
mostrar abre_set largo abre_set ejercicios cierra_set cierra_set
```

## Errores representativos

- Lexico: `Token desconocido: '$'. GymScript esperaba una palabra valida de entrenamiento.`
- Sintactico: `Se esperaba 'inicio_rutina' despues de la condicion de 'si_fuerza'.`
- Semantico: `La variable 'repeticiones' se uso antes de ser declarada.`
- Runtime: `No se puede dividir la rutina entre cero.`
