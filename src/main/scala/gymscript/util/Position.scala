package gymscript.util

final case class Position(line: Int, column: Int, index: Int) {
  def advance(by: Int = 1): Position = copy(column = column + by, index = index + by)

  def nextLine: Position = Position(line + 1, 1, index + 1)

  def render: String = s"linea $line, columna $column, indice $index"
}

object Position {
  val Start: Position = Position(line = 1, column = 1, index = 0)
}

