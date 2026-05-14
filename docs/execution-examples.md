# Ejemplos de ejecucion

## Ciclo basico

```gymscript
peso repeticiones = 0
peso meta = 3

mientras_entrenas repeticiones menor_que meta
  mostrar(repeticiones)
  repeticiones = repeticiones + 1
fin_rutina

si_fuerza meta mayor_que 2
  mostrar("Rutina completada")
descanso
  mostrar("Rutina incompleta")
fin_rutina
```

Salida:

```text
0
1
2
Rutina completada
```

## Condicional sin else

```gymscript
si_fuerza verdadero y no falso
  mostrar("Condicion valida")
fin_rutina
```

## Comentarios y aritmetica

```gymscript
# Comentario de preparacion
peso total = 1 + 2 * 3
mostrar(total)
mostrar((1 + 2) * 3)
```
