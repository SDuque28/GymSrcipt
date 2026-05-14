# Modulos e imports

GymScript soporta imports basicos de rutinas:

```text
importar_rutina "math.gym"
importar_rutina "utils/messages.gym"
```

## Reglas

- Los imports deben ir al inicio del archivo.
- Solo se permiten rutas relativas.
- El resolver bloquea path traversal como `../../`.
- Se detectan imports duplicados en un mismo archivo.
- Se detectan ciclos de importacion.
- Los modulos importados solo pueden exponer rutinas en el nivel superior.

## Resolucion

1. Se parsea el archivo principal.
2. Se resuelven imports en DFS.
3. Se agregan las rutinas importadas al AST final.
4. Se ejecuta el analisis semantico sobre el programa combinado.
