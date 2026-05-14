# Manejo de errores

GymScript usa un formato uniforme:

- `[LEXICAL ERROR] line X, column Y: mensaje`
- `[PARSE ERROR] line X, column Y: mensaje`
- `[SEMANTIC ERROR] line X, column Y: mensaje`
- `[RUNTIME ERROR] line X, column Y: mensaje`

## Casos cubiertos

- Token desconocido
- String sin cierre
- Numero decimal mal formado
- Simbolos legacy en modo estricto
- Falta de `inicio_rutina` o `fin_rutina`
- `descanso` fuera de contexto
- Variable no declarada
- Rutina no declarada
- Aridad invalida
- Tipo de argumento invalido
- Tipo de retorno invalido
- Uso no valido de listas
- Division por cero
- Indices fuera de rango
- Imports inseguros, duplicados o ciclicos
