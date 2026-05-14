package gymscript.resolver

sealed trait GymType {
  def displayName: String
}

object GymType {
  case object NumberType extends GymType {
    val displayName = "numero"
  }

  case object StringType extends GymType {
    val displayName = "texto"
  }

  case object BooleanType extends GymType {
    val displayName = "booleano"
  }

  case object NullType extends GymType {
    val displayName = "sin_resultado"
  }

  case object UnknownType extends GymType {
    val displayName = "desconocido"
  }

  final case class ListType(elementType: GymType) extends GymType {
    val displayName: String = elementType match {
      case UnknownType => "lista_de desconocido"
      case other => s"lista_de ${other.displayName}"
    }
  }
}
