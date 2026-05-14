package gymscript.parser

import gymscript.util.Position

final case class ParseError(message: String, position: Position) {
  def render: String = s"Error sintactico en ${position.render}: $message"
}

