package gymscript.interpreter

import gymscript.util.Position

import scala.collection.mutable

final class Environment(
    private val values: mutable.Map[String, Value] = mutable.Map.empty,
    private val parent: Option[Environment] = None
) {

  def define(name: String, value: Value): Environment = {
    values.update(name, value)
    this
  }

  def assign(name: String, value: Value, position: Position): Either[RuntimeError, Environment] = {
    if (values.contains(name)) {
      values.update(name, value)
      Right(this)
    } else {
      parent match {
        case Some(enclosing) =>
          enclosing.assign(name, value, position).map(_ => this)

        case None =>
          Left(RuntimeError(s"La variable '$name' no ha sido declarada.", position))
      }
    }
  }

  def resolve(name: String, position: Position): Either[RuntimeError, Value] = {
    values.get(name) match {
      case Some(value) => Right(value)
      case None =>
        parent match {
          case Some(enclosing) => enclosing.resolve(name, position)
          case None => Left(RuntimeError(s"La variable '$name' no existe en el entorno actual.", position))
        }
    }
  }

  def child: Environment = new Environment(mutable.Map.empty, Some(this))
}
