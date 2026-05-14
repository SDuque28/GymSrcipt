package gymscript.resolver

import gymscript.parser.Program
import gymscript.util.Position

final case class SemanticError(message: String, position: Position) {
  def render: String = s"Error semantico en ${position.render}: $message"
}

final class SemanticAnalyzer {
  def analyze(program: Program): Either[List[SemanticError], Program] = {
    Right(program)
  }
}

