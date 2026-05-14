package gymscript.lexer

import gymscript.util.Position
import org.scalatest.funsuite.AnyFunSuite

final class LexerSpec extends AnyFunSuite {
  private val lexer = new Lexer()

  test("tokenize reconoce una declaracion simple") {
    val result = lexer.tokenize("peso meta = 3\n")

    assert(result.isRight)
    val tokens = result.toOption.get
    assert(tokens.exists(_.tokenType == TokenType.Peso))
    assert(tokens.exists(_.tokenType == TokenType.Identifier))
    assert(tokens.exists(_.tokenType == TokenType.Number))
    assert(tokens.last.tokenType == TokenType.EOF)
  }

  test("tokenize strings con escapes basicos") {
    val result = lexer.tokenize("mostrar(\"Linea\\n1\")")

    assert(result.isRight)
    val stringToken = result.toOption.get.find(_.tokenType == TokenType.StringLiteral).get
    assert(stringToken.literal.contains("Linea\n1"))
  }

  test("ignora comentarios de linea") {
    val result = lexer.tokenize("# comentario\npeso meta = 1")

    assert(result.isRight)
    val tokens = result.toOption.get
    assert(!tokens.exists(_.tokenType == TokenType.Comment))
    assert(tokens.count(_.tokenType == TokenType.Peso) == 1)
  }

  test("detecta string no cerrado") {
    val result = lexer.tokenize("\"Rutina")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("String sin cierre")))
  }

  test("detecta caracter invalido") {
    val result = lexer.tokenize("@")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("Caracter no reconocido")))
  }

  test("detecta decimal invalido") {
    val result = lexer.tokenize("10.5.3")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("Numero decimal mal formado")))
  }

  test("mantiene linea y columna") {
    val result = lexer.tokenize("peso meta = 1\nmostrar(meta)")

    assert(result.isRight)
    val mostrarToken = result.toOption.get.find(_.tokenType == TokenType.Mostrar).get
    assert(mostrarToken.position == Position(2, 1, 14))
  }
}
