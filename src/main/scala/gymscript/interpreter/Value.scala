package gymscript.interpreter

import gymscript.parser.Block

sealed trait Value {
  def render: String
}

final case class NumberValue(value: BigDecimal) extends Value {
  override def render: String = value.bigDecimal.stripTrailingZeros().toPlainString
}

final case class StringValue(value: String) extends Value {
  override def render: String = value
}

final case class BooleanValue(value: Boolean) extends Value {
  override def render: String = value.toString
}

final case class ListValue(values: Vector[Value]) extends Value {
  override def render: String = values.map(_.render).mkString("[", ", ", "]")
}

final case class RoutineValue(name: String, parameters: List[String], body: Block, closure: Environment) extends Value {
  override def render: String = s"<rutina $name>"
}

case object NullValue extends Value {
  override def render: String = "null"
}
