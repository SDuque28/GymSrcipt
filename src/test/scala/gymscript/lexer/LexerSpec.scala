package gymscript.lexer

import org.scalatest.funsuite.AnyFunSuite

final class LexerSpec extends AnyFunSuite {
  test("tokenize reconoce una declaracion simple") {
    val lexer = new Lexer()
    val result = lexer.tokenize("peso meta = 3\n")

    assert(result.isRight)
    val tokens = result.toOption.get
    assert(tokens.exists(_.tokenType == TokenType.Peso))
    assert(tokens.exists(_.tokenType == TokenType.Identifier))
    assert(tokens.exists(_.tokenType == TokenType.Number))
    assert(tokens.last.tokenType == TokenType.EOF)
  }
}

