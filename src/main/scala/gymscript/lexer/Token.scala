package gymscript.lexer

import gymscript.util.Position

final case class Token(
    tokenType: TokenType,
    lexeme: String,
    position: Position,
    literal: Option[String] = None
)

