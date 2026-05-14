# Tabla lexica final

## Palabras reservadas

| Lexema | Token |
| --- | --- |
| `peso` | `Peso` |
| `mostrar` | `Mostrar` |
| `si_fuerza` | `SiFuerza` |
| `descanso` | `Descanso` |
| `mientras_entrenas` | `MientrasEntrenas` |
| `inicio_rutina` | `InicioRutina` |
| `fin_rutina` | `FinRutina` |
| `rutina` | `Rutina` |
| `llamar` | `Llamar` |
| `entregar_resultado` | `EntregarResultado` |
| `subir_peso` | `SubirPeso` |
| `bajar_peso` | `BajarPeso` |
| `por` | `Por` |
| `lista` | `Lista` |
| `tomar` | `Tomar` |
| `largo` | `Largo` |
| `cambiar_set` | `CambiarSet` |
| `agregar_set` | `AgregarSet` |
| `quitar_set` | `QuitarSet` |
| `rango_set` | `RangoSet` |
| `sin_resultado` | `SinResultado` |
| `verdadero` | `Verdadero` |
| `falso` | `Falso` |

## Operadores tematicos

| Lexema | Token |
| --- | --- |
| `mas_reps` | `Plus` |
| `menos_reps` | `Minus` |
| `series_de` | `Star` |
| `dividir_rutina` | `Slash` |
| `cargar` | `Assign` |
| `levanta_mas_que` | `GreaterThan` |
| `levanta_menos_que` | `LessThan` |
| `levanta_igual_que` | `EqualEqual` |
| `no_levanta_igual` | `BangEqual` |
| `levanta_minimo` | `GreaterEqual` |
| `levanta_maximo` | `LessEqual` |
| `y_entrena` | `And` |
| `o_descansa` | `Or` |
| `sin_energia` | `Not` |

## Delimitadores tematicos

| Lexema | Token |
| --- | --- |
| `abre_set` | `LeftParen` |
| `cierra_set` | `RightParen` |
| `separa` | `Comma` |

## Legacy opcional

En modo `--legacy` tambien se aceptan:

- `+ - * /`
- `= == != > < >= <=`
- `(` `)` `,`
- `mayor_que`, `menor_que`, `mayor_igual`, `menor_igual`
- `y`, `o`, `no`
