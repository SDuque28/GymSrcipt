package gymscript.interpreter

import gymscript.parser.{ Block, RoutineParameter }

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

final case class ListValue(values: Vector[Value], elementTypeName: Option[String] = None) extends Value {
  override def render: String = values.map(_.render).mkString("[", ", ", "]")
}

final case class RoutineValue(name: String, parameters: List[RoutineParameter], body: Block, closure: Environment) extends Value {
  override def render: String = s"<rutina $name>"
}

case object NullValue extends Value {
  override def render: String = "sin_resultado"
}
