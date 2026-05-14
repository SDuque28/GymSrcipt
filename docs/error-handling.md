# Manejo de errores

GymScript usa un formato uniforme:

- `[LEXICAL ERROR] line X, column Y: mensaje`
- `[PARSE ERROR] line X, column Y: mensaje`
- `[SEMANTIC ERROR] line X, column Y: mensaje`
- `[RUNTIME ERROR] line X, column Y: mensaje`

## Casos cubiertos

- Tokens legacy en modo estricto
- Strings no cerrados
- Argumentos faltantes
- `entregar_resultado` fuera de rutina
- Rutina inexistente
- Aridad incorrecta
- Variables no declaradas
- Listas con tipos incompatibles
- Indices fuera de rango
- Division por cero
- Recursion o loops por encima de limites de seguridad
