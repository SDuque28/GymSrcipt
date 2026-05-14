# Gramatica implementada

```bnf
<programa> ::= <sentencia>* EOF

<sentencia> ::= <declaracion>
              | <asignacion>
              | <salida>
              | <condicional>
              | <ciclo>
              | <rutina>
              | <llamada>
              | <expresion>

<declaracion> ::= "peso" IDENTIFICADOR "cargar" <expresion>
<asignacion> ::= IDENTIFICADOR "cargar" <expresion>

<salida> ::= "mostrar" "abre_set" <expresion> "cierra_set"

<condicional> ::= "si_fuerza" <expresion> "inicio_rutina"
                  <bloque>
                  ["descanso" "inicio_rutina" <bloque>]
                  "fin_rutina"

<ciclo> ::= "mientras_entrenas" <expresion> "inicio_rutina"
            <bloque>
            "fin_rutina"

<rutina> ::= "rutina" IDENTIFICADOR "abre_set" [<parametros>] "cierra_set" "inicio_rutina"
             <bloque>
             "fin_rutina"

<llamada> ::= "llamar" IDENTIFICADOR "abre_set" [<argumentos>] "cierra_set"

<parametros> ::= IDENTIFICADOR ("separa" IDENTIFICADOR)*
<argumentos> ::= <expresion> ("separa" <expresion>)*
<bloque> ::= <sentencia>+

<expresion> ::= <or>
<or> ::= <and> ("o_descansa" <and>)*
<and> ::= <igualdad> ("y_entrena" <igualdad>)*
<igualdad> ::= <comparacion> (("levanta_igual_que" | "no_levanta_igual") <comparacion>)*
<comparacion> ::= <termino> (("levanta_mas_que" | "levanta_menos_que" | "levanta_minimo" | "levanta_maximo") <termino>)*
<termino> ::= <factor> (("mas_reps" | "menos_reps") <factor>)*
<factor> ::= <unario> (("series_de" | "dividir_rutina") <unario>)*
<unario> ::= ("sin_energia" | "menos_reps") <unario> | <primario>
<primario> ::= NUMERO
             | STRING
             | "verdadero"
             | "falso"
             | IDENTIFICADOR
             | "abre_set" <expresion> "cierra_set"
             | "lista" "abre_set" [<argumentos>] "cierra_set"
             | "tomar" "abre_set" <expresion> "separa" <expresion> "cierra_set"
             | "largo" "abre_set" <expresion> "cierra_set"
```

## Notas

- Los bloques de `si_fuerza`, `descanso`, `mientras_entrenas` y `rutina` exigen `inicio_rutina`.
- Los bloques cierran con `fin_rutina`.
- Se mantiene una compatibilidad legacy acotada solo para migracion.
