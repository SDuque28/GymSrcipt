# Gramatica implementada (BNF de trabajo)

```bnf
<programa> ::= <sentencia>* EOF

<sentencia> ::= <declaracion>
              | <asignacion>
              | <mostrar>
              | <si>
              | <mientras>
              | <bloque>
              | <expresion>

<declaracion> ::= "peso" <identificador> ["=" <expresion>]

<asignacion> ::= <identificador> "=" <expresion>

<mostrar> ::= "mostrar" "(" <expresion> ")"

<si> ::= "si_fuerza" <expresion> NEWLINE <sentencia>* ["descanso" NEWLINE <sentencia>*] "fin_rutina"

<mientras> ::= "mientras_entrenas" <expresion> NEWLINE <sentencia>* "fin_rutina"

<bloque> ::= "{" <sentencia>* "}"

<expresion> ::= <or>
<or> ::= <and> ("o" <and>)*
<and> ::= <igualdad> ("y" <igualdad>)*
<igualdad> ::= <comparacion> (("==" | "!=") <comparacion>)*
<comparacion> ::= <termino> (("mayor_que" | "menor_que" | "mayor_igual" | "menor_igual") <termino>)*
<termino> ::= <factor> (("+" | "-") <factor>)*
<factor> ::= <unario> (("*" | "/") <unario>)*
<unario> ::= ("no" | "-") <unario> | <primario>
<primario> ::= <literal> | <identificador> | "(" <expresion> ")"
```

Notas:

- Los comentarios se eliminan en el lexer.
- Los saltos de linea son relevantes para separar cabeceras de bloques.
