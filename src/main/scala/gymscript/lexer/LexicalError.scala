package gymscript.lexer

import gymscript.util.Position

final case class LexicalError(message: String, position: Position, lexeme: Option[String] = None) {
  def render: String = {
    val lexemeDetail = lexeme.map(value => s" Lexema: '$value'.").getOrElse("")
    s"Error lexico en ${position.render}: $message$lexemeDetail"
  }
}
