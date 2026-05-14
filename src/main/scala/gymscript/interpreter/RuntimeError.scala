package gymscript.interpreter

import gymscript.util.Position

final case class RuntimeError(message: String, position: Position) {
  def render: String = s"Error de ejecucion en ${position.render}: $message"
}

