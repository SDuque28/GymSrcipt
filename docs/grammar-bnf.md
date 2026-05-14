# Gramática inicial (BNF de trabajo)

```bnf
<programa> ::= <sentencia>* EOF

<sentencia> ::= <declaracion>
              | <asignacion>
              | <mostrar>
              | <si>
              | <mientras>
              | <bloque>

<declaracion> ::= "peso" <identificador> "=" <expresion>
                | "peso" <identificador>

<asignacion> ::= <identificador> "=" <expresion>

<mostrar> ::= "mostrar" "(" <expresion> ")"

<si> ::= "si_fuerza" <expresion> <bloque_inline> ["descanso" <bloque_inline>] "fin_rutina"

<mientras> ::= "mientras_entrenas" <expresion> <bloque_inline> "fin_rutina"

<bloque> ::= "{" <sentencia>* "}"

<expresion> ::= <literal>
              | <identificador>
              | <expresion> <operador_binario> <expresion>
              | <operador_unario> <expresion>
              | "(" <expresion> ")"
```

Notas:

- Esta gramática es una base de diseño, no la versión final.
- El parser definitivo deberá formalizar precedencia y asociatividad.

