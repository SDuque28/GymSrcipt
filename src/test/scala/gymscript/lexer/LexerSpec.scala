package gymscript.lexer

import gymscript.util.Position
import org.scalatest.funsuite.AnyFunSuite

final class LexerSpec extends AnyFunSuite {
  private val lexer = new Lexer()

  test("reconoce operadores tematicos") {
    val source =
      "peso total cargar 1 mas_reps 2 menos_reps 3 series_de 4 dividir_rutina 2\n" +
        "si_fuerza verdadero y_entrena falso o_descansa verdadero inicio_rutina\nfin_rutina"

    val result = lexer.tokenize(source)

    assert(result.isRight)
    val tokenTypes = result.toOption.get.map(_.tokenType)
    assert(tokenTypes.contains(TokenType.Assign))
    assert(tokenTypes.contains(TokenType.Plus))
    assert(tokenTypes.contains(TokenType.Minus))
    assert(tokenTypes.contains(TokenType.Star))
    assert(tokenTypes.contains(TokenType.Slash))
    assert(tokenTypes.contains(TokenType.And))
    assert(tokenTypes.contains(TokenType.Or))
    assert(tokenTypes.contains(TokenType.InicioRutina))
    assert(tokenTypes.contains(TokenType.FinRutina))
  }

  test("reconoce delimitadores tematicos") {
    val result = lexer.tokenize("mostrar abre_set lista abre_set 1 separa 2 cierra_set cierra_set")

    assert(result.isRight)
    val tokenTypes = result.toOption.get.map(_.tokenType)
    assert(tokenTypes.count(_ == TokenType.LeftParen) == 2)
    assert(tokenTypes.count(_ == TokenType.RightParen) == 2)
    assert(tokenTypes.contains(TokenType.Comma))
    assert(tokenTypes.contains(TokenType.Lista))
  }

  test("reconoce cargar como asignacion") {
    val result = lexer.tokenize("peso meta cargar 3")

    assert(result.isRight)
    assert(result.toOption.get.exists(_.tokenType == TokenType.Assign))
  }

  test("ignora comentarios") {
    val result = lexer.tokenize("# comentario\npeso meta cargar 1")

    assert(result.isRight)
    assert(!result.toOption.get.exists(_.tokenType == TokenType.Comment))
  }

  test("mantiene compatibilidad temporal con simbolos legacy") {
    val result = lexer.tokenize("mostrar(1 + 2)")

    assert(result.isRight)
    val tokenTypes = result.toOption.get.map(_.tokenType)
    assert(tokenTypes.contains(TokenType.LeftParen))
    assert(tokenTypes.contains(TokenType.Plus))
    assert(tokenTypes.contains(TokenType.RightParen))
  }

  test("detecta string no cerrado") {
    val result = lexer.tokenize("\"Rutina")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("String sin cierre")))
  }

  test("mantiene linea y columna con sintaxis tematica") {
    val result = lexer.tokenize("peso meta cargar 1\nmostrar abre_set meta cierra_set")

    assert(result.isRight)
    val mostrarToken = result.toOption.get.find(_.tokenType == TokenType.Mostrar).get
    assert(mostrarToken.position == Position(2, 1, 19))
  }
}
