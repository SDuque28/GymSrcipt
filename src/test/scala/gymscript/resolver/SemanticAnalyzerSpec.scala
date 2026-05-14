package gymscript.resolver

import gymscript.lexer.Lexer
import gymscript.parser.Parser
import org.scalatest.funsuite.AnyFunSuite

final class SemanticAnalyzerSpec extends AnyFunSuite {
  private val lexer = new Lexer()
  private val parser = new Parser()
  private val analyzer = new SemanticAnalyzer()

  private def analyze(source: String): Either[List[SemanticError], gymscript.parser.Program] = {
    val tokens = lexer.tokenize(source).toOption.get
    val program = parser.parse(tokens).toOption.get
    analyzer.analyze(program)
  }

  test("detecta variable no declarada") {
    val result = analyze("mostrar(meta)")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("no ha sido declarada")))
  }

  test("detecta redeclaracion en el mismo alcance") {
    val result = analyze("peso meta = 1\npeso meta = 2")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("ya fue declarada")))
  }

  test("permite reasignacion valida") {
    val result = analyze("peso meta = 1\nmeta = 2")

    assert(result.isRight)
  }

  test("valida variables dentro de bloques") {
    val source =
      """si_fuerza verdadero
        |  peso interno = 1
        |fin_rutina
        |mostrar(interno)""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("interno")))
  }
}
