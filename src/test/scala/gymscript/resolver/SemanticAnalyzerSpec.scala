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

  test("detecta return fuera de rutina") {
    val result = analyze("entregar_resultado 1")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("entregar_resultado")))
  }

  test("detecta rutina no declarada") {
    val result = analyze("peso total cargar llamar sumar abre_set 1 separa 2 cierra_set")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("rutina 'sumar'")))
  }

  test("detecta numero incorrecto de argumentos") {
    val source =
      """rutina sumar abre_set a separa b cierra_set inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina
        |peso total cargar llamar sumar abre_set 1 cierra_set""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("esperaba 2 argumento")))
  }

  test("detecta parametros duplicados") {
    val source =
      """rutina sumar abre_set a separa a cierra_set inicio_rutina
        |  entregar_resultado a
        |fin_rutina""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("parametro")))
  }

  test("detecta variable no declarada") {
    val result = analyze("mostrar abre_set meta cierra_set")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("meta")))
  }

  test("detecta redeclaracion") {
    val result = analyze("peso meta cargar 1\npeso meta cargar 2")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("ya fue declarada")))
  }

  test("detecta lista con tipos incompatibles") {
    val result = analyze("""peso ejercicios cargar lista abre_set "curl" separa 10 cierra_set""")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("mezcla tipos incompatibles")))
  }

  test("detecta operaciones invalidas sobre no listas") {
    val result = analyze("peso ejercicios cargar 1\nmostrar abre_set largo abre_set ejercicios cierra_set cierra_set")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("lista")))
  }
}
