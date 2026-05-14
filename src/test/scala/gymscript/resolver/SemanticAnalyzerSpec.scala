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

  test("valida variables declaradas") {
    val result = analyze("peso meta cargar 1\nmostrar abre_set meta cierra_set")

    assert(result.isRight)
  }

  test("valida scopes de bloque") {
    val source =
      """si_fuerza verdadero inicio_rutina
        |  peso interno cargar 1
        |fin_rutina
        |mostrar abre_set interno cierra_set""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("interno")))
  }

  test("valida funciones y parametros") {
    val source =
      """rutina saludar abre_set nombre cierra_set inicio_rutina
        |  mostrar abre_set nombre cierra_set
        |fin_rutina
        |llamar saludar abre_set "Ana" cierra_set""".stripMargin

    val result = analyze(source)

    assert(result.isRight)
  }

  test("detecta aridad invalida en rutina") {
    val source =
      """rutina saludar abre_set nombre cierra_set inicio_rutina
        |  mostrar abre_set nombre cierra_set
        |fin_rutina
        |llamar saludar abre_set "Ana" separa "Extra" cierra_set""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("esperaba 1 argumento")))
  }

  test("valida listas si se implementan") {
    val result = analyze("peso ejercicios cargar lista abre_set \"curl\" separa \"press\" cierra_set\nmostrar abre_set largo abre_set ejercicios cierra_set cierra_set")

    assert(result.isRight)
  }
}
