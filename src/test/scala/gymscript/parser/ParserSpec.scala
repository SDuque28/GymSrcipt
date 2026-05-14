package gymscript.parser

import gymscript.lexer.{ Lexer, TokenType }
import org.scalatest.funsuite.AnyFunSuite

final class ParserSpec extends AnyFunSuite {
  private val lexer = new Lexer()

  private def parseSource(source: String): Either[List[ParseError], Program] = {
    val parser = new Parser()
    val tokens = lexer.tokenize(source).toOption.get
    parser.parse(tokens)
  }

  test("parsea declaracion con cargar") {
    val result = parseSource("peso meta cargar 3")

    assert(result.isRight)
    assert(result.toOption.get.statements.head.isInstanceOf[VariableDeclaration])
  }

  test("parsea mostrar con abre_set y cierra_set") {
    val result = parseSource("""mostrar abre_set "ok" cierra_set""")

    assert(result.isRight)
    assert(result.toOption.get.statements.head.isInstanceOf[PrintStatement])
  }

  test("parsea if con inicio_rutina y fin_rutina") {
    val source =
      """si_fuerza verdadero inicio_rutina
        |  mostrar abre_set "a" cierra_set
        |descanso inicio_rutina
        |  mostrar abre_set "b" cierra_set
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[IfStatement]
    assert(statement.elseBranch.nonEmpty)
  }

  test("parsea while con inicio_rutina y fin_rutina") {
    val source =
      """peso i cargar 0
        |mientras_entrenas i levanta_menos_que 2 inicio_rutina
        |  i cargar i mas_reps 1
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    assert(result.toOption.get.statements.exists(_.isInstanceOf[WhileStatement]))
  }

  test("respeta precedencia con operadores tematicos") {
    val result = parseSource("mostrar abre_set 1 mas_reps 2 series_de 3 cierra_set")

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[PrintStatement]
    val expression = statement.expression.asInstanceOf[BinaryExpression]
    assert(expression.operator == TokenType.Plus)
    assert(expression.right.isInstanceOf[BinaryExpression])
    assert(expression.right.asInstanceOf[BinaryExpression].operator == TokenType.Star)
  }

  test("parsea expresiones anidadas") {
    val result = parseSource("mostrar abre_set abre_set 1 mas_reps 2 cierra_set series_de 3 cierra_set")

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[PrintStatement]
    assert(statement.expression.isInstanceOf[BinaryExpression])
  }

  test("parsea rutina y llamada") {
    val source =
      """rutina saludar abre_set nombre cierra_set inicio_rutina
        |  mostrar abre_set nombre cierra_set
        |fin_rutina
        |llamar saludar abre_set "Ana" cierra_set""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    assert(result.toOption.get.statements.head.isInstanceOf[RoutineDeclaration])
    assert(result.toOption.get.statements(1).isInstanceOf[CallStatement])
  }

  test("parsea listas y accesos tematicos") {
    val result = parseSource("""mostrar abre_set tomar abre_set lista abre_set 1 separa 2 cierra_set separa 0 cierra_set cierra_set""")

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[PrintStatement]
    assert(statement.expression.isInstanceOf[TakeExpression])
  }

  test("reporta error si falta inicio_rutina") {
    val source =
      """si_fuerza verdadero
        |  mostrar abre_set "a" cierra_set
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("inicio_rutina")))
  }
}
