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
  case object FinRutina extends TokenType { val lexemeName = "fin_rutina" }
  case object Verdadero extends TokenType { val lexemeName = "verdadero" }
  case object Falso extends TokenType { val lexemeName = "falso" }

  case object Identifier extends TokenType { val lexemeName = "identifier" }
  case object Number extends TokenType { val lexemeName = "number" }
  case object StringLiteral extends TokenType { val lexemeName = "string" }

  case object Plus extends TokenType { val lexemeName = "+" }
  case object Minus extends TokenType { val lexemeName = "-" }
  case object Star extends TokenType { val lexemeName = "*" }
  case object Slash extends TokenType { val lexemeName = "/" }
  case object Assign extends TokenType { val lexemeName = "=" }
  case object EqualEqual extends TokenType { val lexemeName = "==" }
  case object BangEqual extends TokenType { val lexemeName = "!=" }
  case object GreaterThan extends TokenType { val lexemeName = "mayor_que" }
  case object LessThan extends TokenType { val lexemeName = "menor_que" }
  case object GreaterEqual extends TokenType { val lexemeName = "mayor_igual" }
  case object LessEqual extends TokenType { val lexemeName = "menor_igual" }
  case object And extends TokenType { val lexemeName = "y" }
  case object Or extends TokenType { val lexemeName = "o" }
  case object Not extends TokenType { val lexemeName = "no" }

  case object LeftParen extends TokenType { val lexemeName = "(" }
  case object RightParen extends TokenType { val lexemeName = ")" }
  case object LeftBrace extends TokenType { val lexemeName = "{" }
  case object RightBrace extends TokenType { val lexemeName = "}" }
  case object Comma extends TokenType { val lexemeName = "," }
  case object NewLine extends TokenType { val lexemeName = "\\n" }
  case object Comment extends TokenType { val lexemeName = "comment" }
  case object EOF extends TokenType { val lexemeName = "eof" }

  val keywords: Map[String, TokenType] = Map(
    "peso" -> Peso,
    "mostrar" -> Mostrar,
    "si_fuerza" -> SiFuerza,
    "descanso" -> Descanso,
    "mientras_entrenas" -> MientrasEntrenas,
    "fin_rutina" -> FinRutina,
    "verdadero" -> Verdadero,
    "falso" -> Falso,
    "mayor_que" -> GreaterThan,
    "menor_que" -> LessThan,
    "mayor_igual" -> GreaterEqual,
    "menor_igual" -> LessEqual,
    "y" -> And,
    "o" -> Or,
    "no" -> Not
  )
}

