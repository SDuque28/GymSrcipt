# Ejemplos de ejecución

## Ciclo básico

```gymscript
peso repeticiones = 0
peso meta = 3

mientras_entrenas repeticiones menor_que meta
  mostrar(repeticiones)
  repeticiones = repeticiones + 1
fin_rutina
```

Salida esperada cuando el parser e intérprete estén completos:

```text
0
1
2
```

## Condicional

```gymscript
si_fuerza verdadero
  mostrar("Rutina completada")
descanso
  mostrar("Rutina incompleta")
fin_rutina
```

## Comentarios

```gymscript
# Comentario de preparación
peso activo = verdadero
mostrar("Inicio de sesión")
```

