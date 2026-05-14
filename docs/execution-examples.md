# Ejemplos de ejecucion

## Llamada con retorno

```gymscript
rutina sumar abre_set a separa b cierra_set inicio_rutina
  entregar_resultado a mas_reps b
fin_rutina

peso total cargar llamar sumar abre_set 2 separa 3 cierra_set
mostrar abre_set total cierra_set
```

Salida:

```text
5
```

## Ajuste de peso

```gymscript
peso repeticiones cargar 1
subir_peso repeticiones por 4
bajar_peso repeticiones
mostrar abre_set repeticiones cierra_set
```

## Listas

```gymscript
peso ejercicios cargar lista abre_set "curl" separa "press" cierra_set
agregar_set abre_set ejercicios separa "dominadas" cierra_set
cambiar_set abre_set ejercicios separa 0 separa "sentadilla" cierra_set
mostrar abre_set ejercicios cierra_set
mostrar abre_set tomar abre_set ejercicios separa 1 cierra_set cierra_set
mostrar abre_set largo abre_set ejercicios cierra_set cierra_set
```
