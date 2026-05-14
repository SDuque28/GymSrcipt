package gymscript.interpreter

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

case object NullValue extends Value {
  override def render: String = "null"
}

