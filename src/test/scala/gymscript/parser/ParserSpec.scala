package gymscript.parser

import gymscript.lexer.{ Token, TokenType }
import gymscript.util.Position
import org.scalatest.funsuite.AnyFunSuite

final class ParserSpec extends AnyFunSuite {
  test("parse devuelve un programa base con una secuencia minima de tokens") {
    val parser = new Parser()
    val tokens = List(Token(TokenType.EOF, "", Position.Start))

    val result = parser.parse(tokens)

    assert(result.isRight)
    assert(result.toOption.get.statements.isEmpty)
  }
}

