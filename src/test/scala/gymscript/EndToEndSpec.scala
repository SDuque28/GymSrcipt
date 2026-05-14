package gymscript

import gymscript.interpreter.Interpreter
import gymscript.lexer.Lexer
import gymscript.parser.Parser
import gymscript.resolver.SemanticAnalyzer
import org.scalatest.funsuite.AnyFunSuite

final class EndToEndSpec extends AnyFunSuite {
  test("ejecuta un source completo hasta obtener salida final") {
    val source =
      """peso meta = 2
        |peso actual = 0
        |
        |mientras_entrenas actual menor_que meta
        |  mostrar(actual)
        |  actual = actual + 1
        |fin_rutina
        |
        |si_fuerza actual == meta
        |  mostrar("completo")
        |fin_rutina""".stripMargin

    val lexer = new Lexer()
    val parser = new Parser()
    val analyzer = new SemanticAnalyzer()
    val interpreter = new Interpreter()

    val result =
      for {
        tokens <- lexer.tokenize(source)
        program <- parser.parse(tokens)
        verified <- analyzer.analyze(program)
        outputs <- interpreter.execute(verified)
      } yield outputs

    assert(result == Right(List("0", "1", "completo")))
  }
}
