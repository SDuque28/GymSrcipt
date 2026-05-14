package gymscript.interpreter

import gymscript.util.Position

final case class RuntimeError(message: String, position: Position, context: Option[String] = None) {
  def render: String = {
    val contextDetail = context.map(value => s" Context: '$value'.").getOrElse("")
    s"[RUNTIME ERROR] line ${position.line}, column ${position.column}: $message$contextDetail"
  }
}
