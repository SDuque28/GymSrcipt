package gymscript.parser

import gymscript.lexer.{ Token, TokenType }
import gymscript.util.Position

final class Parser {
  def parse(tokens: List[Token]): Either[List[ParseError], Program] = {
    val meaningfulTokens = tokens.filterNot(_.tokenType == TokenType.Comment)

    meaningfulTokens.headOption match {
      case None =>
        Left(List(ParseError("La lista de tokens esta vacia.", Position.Start)))

      case Some(firstToken) =>
        // TODO: Implementar parser descendente recursivo con precedencia.
        // En esta fase se retorna un programa vacio para validar la estructura del proyecto.
        Right(Program(statements = Nil, position = firstToken.position))
    }
  }
}

