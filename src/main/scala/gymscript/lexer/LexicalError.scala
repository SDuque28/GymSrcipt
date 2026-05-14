package gymscript.lexer

import gymscript.util.Position

final case class LexicalError(message: String, position: Position) {
  def render: String = s"Error lexico en ${position.render}: $message"
}

