# Gramatica final

```bnf
<programa> ::= <sentencia>* EOF

<sentencia> ::= <declaracion>
              | <asignacion>
              | <salida>
              | <condicional>
              | <ciclo>
              | <rutina>
              | <retorno>
              | <ajuste_peso>
              | <mutacion_lista>
              | <llamada>
              | <expresion>

<declaracion> ::= "peso" IDENTIFICADOR "cargar" <expresion>
<asignacion> ::= IDENTIFICADOR "cargar" <expresion>
<salida> ::= "mostrar" "abre_set" <expresion> "cierra_set"
<retorno> ::= "entregar_resultado" [<expresion>]
<ajuste_peso> ::= ("subir_peso" | "bajar_peso") IDENTIFICADOR ["por" <expresion>]

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

<mutacion_lista> ::= "cambiar_set" "abre_set" IDENTIFICADOR "separa" <expresion> "separa" <expresion> "cierra_set"
                   | "agregar_set" "abre_set" IDENTIFICADOR "separa" <expresion> "cierra_set"
                   | "quitar_set" "abre_set" IDENTIFICADOR "separa" <expresion> "cierra_set"

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
             | "sin_resultado"
             | IDENTIFICADOR
             | "llamar" IDENTIFICADOR "abre_set" [<argumentos>] "cierra_set"
             | "abre_set" <expresion> "cierra_set"
             | "lista" "abre_set" [<argumentos>] "cierra_set"
             | "tomar" "abre_set" <expresion> "separa" <expresion> "cierra_set"
             | "largo" "abre_set" <expresion> "cierra_set"
             | "rango_set" "abre_set" <expresion> "separa" <expresion> "separa" <expresion> "cierra_set"
```
