# Tabla lexica implementada

| Categoria | Lexema(s) | Token |
| --- | --- | --- |
| Declaracion | `peso` | `Peso` |
| Salida | `mostrar` | `Mostrar` |
| If | `si_fuerza` | `SiFuerza` |
| Else | `descanso` | `Descanso` |
| While | `mientras_entrenas` | `MientrasEntrenas` |
| Fin de bloque | `fin_rutina` | `FinRutina` |
| Booleano | `verdadero` | `Verdadero` |
| Booleano | `falso` | `Falso` |
| Identificador | `meta`, `repeticiones`, `_contador` | `Identifier` |
| Numero | `10`, `3.5` | `Number` |
| String | `"Rutina completada"` | `StringLiteral` |
| Suma | `+` | `Plus` |
| Resta | `-` | `Minus` |
| Multiplicacion | `*` | `Star` |
| Division | `/` | `Slash` |
| Asignacion | `=` | `Assign` |
| Igualdad | `==` | `EqualEqual` |
| Diferente | `!=` | `BangEqual` |
| Mayor | `mayor_que` | `GreaterThan` |
| Menor | `menor_que` | `LessThan` |
| Mayor o igual | `mayor_igual` | `GreaterEqual` |
| Menor o igual | `menor_igual` | `LessEqual` |
| Conjuncion | `y` | `And` |
| Disyuncion | `o` | `Or` |
| Negacion | `no` | `Not` |
| Delimitador | `(` | `LeftParen` |
| Delimitador | `)` | `RightParen` |
| Delimitador | `{` | `LeftBrace` |
| Delimitador | `}` | `RightBrace` |
| Separador | `,` | `Comma` |
| Comentario | `# comentario` | ignorado por el lexer |
| Fin de linea | `\n` | `NewLine` |
| Fin de archivo | EOF | `EOF` |

## Casos de error lexico detectados

- Caracter desconocido
- String no cerrado
- Secuencia de escape no soportada
- Decimal mal formado, por ejemplo `10.5.3`
- Identificador invalido que inicia con numero, por ejemplo `1variable`
