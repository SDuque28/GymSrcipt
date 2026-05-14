package gymscript.lexer

sealed trait TokenType {
  def lexemeName: String
}

object TokenType {
  case object Peso extends TokenType { val lexemeName = "peso" }
  case object Mostrar extends TokenType { val lexemeName = "mostrar" }
  case object SiFuerza extends TokenType { val lexemeName = "si_fuerza" }
  case object Descanso extends TokenType { val lexemeName = "descanso" }
  case object MientrasEntrenas extends TokenType { val lexemeName = "mientras_entrenas" }
  case object InicioRutina extends TokenType { val lexemeName = "inicio_rutina" }
  case object FinRutina extends TokenType { val lexemeName = "fin_rutina" }
  case object Rutina extends TokenType { val lexemeName = "rutina" }
  case object Llamar extends TokenType { val lexemeName = "llamar" }
  case object EntregarResultado extends TokenType { val lexemeName = "entregar_resultado" }
  case object SubirPeso extends TokenType { val lexemeName = "subir_peso" }
  case object BajarPeso extends TokenType { val lexemeName = "bajar_peso" }
  case object Por extends TokenType { val lexemeName = "por" }
  case object Lista extends TokenType { val lexemeName = "lista" }
  case object Tomar extends TokenType { val lexemeName = "tomar" }
  case object Largo extends TokenType { val lexemeName = "largo" }
  case object CambiarSet extends TokenType { val lexemeName = "cambiar_set" }
  case object AgregarSet extends TokenType { val lexemeName = "agregar_set" }
  case object QuitarSet extends TokenType { val lexemeName = "quitar_set" }
  case object RangoSet extends TokenType { val lexemeName = "rango_set" }
  case object SinResultado extends TokenType { val lexemeName = "sin_resultado" }
  case object Verdadero extends TokenType { val lexemeName = "verdadero" }
  case object Falso extends TokenType { val lexemeName = "falso" }

  case object Identifier extends TokenType { val lexemeName = "identifier" }
  case object Number extends TokenType { val lexemeName = "number" }
  case object StringLiteral extends TokenType { val lexemeName = "string" }

  case object Plus extends TokenType { val lexemeName = "mas_reps" }
  case object Minus extends TokenType { val lexemeName = "menos_reps" }
  case object Star extends TokenType { val lexemeName = "series_de" }
  case object Slash extends TokenType { val lexemeName = "dividir_rutina" }
  case object Assign extends TokenType { val lexemeName = "cargar" }
  case object EqualEqual extends TokenType { val lexemeName = "levanta_igual_que" }
  case object BangEqual extends TokenType { val lexemeName = "no_levanta_igual" }
  case object GreaterThan extends TokenType { val lexemeName = "levanta_mas_que" }
  case object LessThan extends TokenType { val lexemeName = "levanta_menos_que" }
  case object GreaterEqual extends TokenType { val lexemeName = "levanta_minimo" }
  case object LessEqual extends TokenType { val lexemeName = "levanta_maximo" }
  case object And extends TokenType { val lexemeName = "y_entrena" }
  case object Or extends TokenType { val lexemeName = "o_descansa" }
  case object Not extends TokenType { val lexemeName = "sin_energia" }

  case object LeftParen extends TokenType { val lexemeName = "abre_set" }
  case object RightParen extends TokenType { val lexemeName = "cierra_set" }
  case object LeftBrace extends TokenType { val lexemeName = "{" }
  case object RightBrace extends TokenType { val lexemeName = "}" }
  case object Comma extends TokenType { val lexemeName = "separa" }
  case object NewLine extends TokenType { val lexemeName = "\\n" }
  case object Comment extends TokenType { val lexemeName = "comment" }
  case object EOF extends TokenType { val lexemeName = "eof" }

  val thematicKeywords: Map[String, TokenType] = Map(
    "peso" -> Peso,
    "mostrar" -> Mostrar,
    "si_fuerza" -> SiFuerza,
    "descanso" -> Descanso,
    "mientras_entrenas" -> MientrasEntrenas,
    "inicio_rutina" -> InicioRutina,
    "fin_rutina" -> FinRutina,
    "rutina" -> Rutina,
    "llamar" -> Llamar,
    "entregar_resultado" -> EntregarResultado,
    "subir_peso" -> SubirPeso,
    "bajar_peso" -> BajarPeso,
    "por" -> Por,
    "lista" -> Lista,
    "tomar" -> Tomar,
    "largo" -> Largo,
    "cambiar_set" -> CambiarSet,
    "agregar_set" -> AgregarSet,
    "quitar_set" -> QuitarSet,
    "rango_set" -> RangoSet,
    "sin_resultado" -> SinResultado,
    "verdadero" -> Verdadero,
    "falso" -> Falso,
    "mas_reps" -> Plus,
    "menos_reps" -> Minus,
    "series_de" -> Star,
    "dividir_rutina" -> Slash,
    "cargar" -> Assign,
    "levanta_mas_que" -> GreaterThan,
    "levanta_menos_que" -> LessThan,
    "levanta_igual_que" -> EqualEqual,
    "no_levanta_igual" -> BangEqual,
    "levanta_minimo" -> GreaterEqual,
    "levanta_maximo" -> LessEqual,
    "y_entrena" -> And,
    "o_descansa" -> Or,
    "sin_energia" -> Not,
    "abre_set" -> LeftParen,
    "cierra_set" -> RightParen,
    "separa" -> Comma
  )

  val legacyKeywords: Map[String, TokenType] = Map(
    "mayor_que" -> GreaterThan,
    "menor_que" -> LessThan,
    "mayor_igual" -> GreaterEqual,
    "menor_igual" -> LessEqual,
    "y" -> And,
    "o" -> Or,
    "no" -> Not
  )
}
