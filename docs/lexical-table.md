# Tabla lexica implementada

| Categoria | Lexema principal | Token |
| --- | --- | --- |
| Declaracion | `peso` | `Peso` |
| Salida | `mostrar` | `Mostrar` |
| If | `si_fuerza` | `SiFuerza` |
| Else | `descanso` | `Descanso` |
| While | `mientras_entrenas` | `MientrasEntrenas` |
| Inicio de bloque | `inicio_rutina` | `InicioRutina` |
| Fin de bloque | `fin_rutina` | `FinRutina` |
| Declaracion de rutina | `rutina` | `Rutina` |
| Llamada de rutina | `llamar` | `Llamar` |
| Lista | `lista` | `Lista` |
| Acceso a lista | `tomar` | `Tomar` |
| Largo de lista | `largo` | `Largo` |
| Booleano | `verdadero` | `Verdadero` |
| Booleano | `falso` | `Falso` |
| Suma | `mas_reps` | `Plus` |
| Resta | `menos_reps` | `Minus` |
| Multiplicacion | `series_de` | `Star` |
| Division | `dividir_rutina` | `Slash` |
| Asignacion | `cargar` | `Assign` |
| Mayor | `levanta_mas_que` | `GreaterThan` |
| Menor | `levanta_menos_que` | `LessThan` |
| Igual | `levanta_igual_que` | `EqualEqual` |
| Diferente | `no_levanta_igual` | `BangEqual` |
| Mayor o igual | `levanta_minimo` | `GreaterEqual` |
| Menor o igual | `levanta_maximo` | `LessEqual` |
| Conjuncion | `y_entrena` | `And` |
| Disyuncion | `o_descansa` | `Or` |
| Negacion | `sin_energia` | `Not` |
| Abre delimitador | `abre_set` | `LeftParen` |
| Cierra delimitador | `cierra_set` | `RightParen` |
| Separador | `separa` | `Comma` |
| Comentario | `# comentario` | ignorado |
| Fin de linea | `\n` | `NewLine` |
| Fin de archivo | EOF | `EOF` |

## Alias legacy soportados

- `+ - * /`
- `= == != > < >= <=`
- `(` `)` `,`
- `mayor_que`, `menor_que`, `mayor_igual`, `menor_igual`
- `y`, `o`, `no`

## Errores lexicos detectados

- Token desconocido
- String no cerrado
- Escape no soportado
- Decimal mal formado
- Identificador invalido que inicia con numero
