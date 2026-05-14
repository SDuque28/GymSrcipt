package gymscript.lexer

import gymscript.util.Position

final case class LexicalError(message: String, position: Position, lexeme: Option[String] = None) {
  def render: String = {
    val lexemeDetail = lexeme.map(value => s" Context: '$value'.").getOrElse("")
    s"[LEXICAL ERROR] line ${position.line}, column ${position.column}: $message$lexemeDetail"
  }
}
