package gymscript.parser

import gymscript.util.Position

final case class ParseError(message: String, position: Position, context: Option[String] = None) {
  def render: String = {
    val contextDetail = context.map(value => s" Context: '$value'.").getOrElse("")
    s"[PARSE ERROR] line ${position.line}, column ${position.column}: $message$contextDetail"
  }
}
