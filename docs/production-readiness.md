# Production Readiness

## Endurecimientos implementados

- CLI con modos `strict` y `legacy`
- Errores uniformes y contextualizados
- Limite de profundidad de llamadas
- Limite de iteraciones en loops
- Analisis semantico de rutinas y listas
- Cobertura automatizada de lexer, parser, semantica, interprete y end-to-end

## Decisiones

- El modo estricto tematico es el default.
- El modo legacy es opcional y controlado por flag.
- Las listas deben ser homogeneas.
- Las rutinas sin `entregar_resultado` devuelven `sin_resultado`.

## Limitaciones reales

- No hay sistema formal de tipos estaticos completos para inferir el tipo de retorno de rutinas.
- No hay multiples archivos ni modulos.
- No hay optimizaciones del interprete ni tail-call optimization.
