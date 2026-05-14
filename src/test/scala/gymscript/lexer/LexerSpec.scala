package gymscript.lexer

import gymscript.util.Position
import org.scalatest.funsuite.AnyFunSuite

final class LexerSpec extends AnyFunSuite {
  private val strictLexer = new Lexer()
  private val legacyLexer = new Lexer(LexerOptions(allowLegacySyntax = true))

  test("reconoce nuevas palabras reservadas") {
    val result = strictLexer.tokenize(
      "entregar_resultado subir_peso bajar_peso tomar largo cambiar_set agregar_set quitar_set rango_set"
    )

    assert(result.isRight)
    val tokenTypes = result.toOption.get.map(_.tokenType)
    assert(tokenTypes.contains(TokenType.EntregarResultado))
    assert(tokenTypes.contains(TokenType.SubirPeso))
    assert(tokenTypes.contains(TokenType.BajarPeso))
    assert(tokenTypes.contains(TokenType.Tomar))
    assert(tokenTypes.contains(TokenType.Largo))
    assert(tokenTypes.contains(TokenType.CambiarSet))
    assert(tokenTypes.contains(TokenType.AgregarSet))
    assert(tokenTypes.contains(TokenType.QuitarSet))
    assert(tokenTypes.contains(TokenType.RangoSet))
  }

  test("reconoce operadores y delimitadores tematicos") {
    val source = "peso total cargar lista abre_set 1 separa 2 cierra_set"
    val result = strictLexer.tokenize(source)

    assert(result.isRight)
    val tokenTypes = result.toOption.get.map(_.tokenType)
    assert(tokenTypes.contains(TokenType.Assign))
    assert(tokenTypes.contains(TokenType.LeftParen))
    assert(tokenTypes.contains(TokenType.Comma))
    assert(tokenTypes.contains(TokenType.RightParen))
  }

  test("rechaza simbolos legacy en modo estricto") {
    val result = strictLexer.tokenize("peso total = 1 + 2")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("modo estricto")))
  }

  test("permite simbolos legacy en modo legacy") {
    val result = legacyLexer.tokenize("peso total = 1 + 2")

    assert(result.isRight)
    val tokenTypes = result.toOption.get.map(_.tokenType)
    assert(tokenTypes.contains(TokenType.Assign))
    assert(tokenTypes.contains(TokenType.Plus))
  }

  test("ignora comentarios") {
    val result = strictLexer.tokenize("# comentario\npeso meta cargar 1")

    assert(result.isRight)
    assert(!result.toOption.get.exists(_.tokenType == TokenType.Comment))
  }

  test("mantiene linea y columna con sintaxis tematica") {
    val result = strictLexer.tokenize("peso meta cargar 1\nmostrar abre_set meta cierra_set")

    assert(result.isRight)
    val mostrarToken = result.toOption.get.find(_.tokenType == TokenType.Mostrar).get
    assert(mostrarToken.position == Position(2, 1, 19))
  }
}
