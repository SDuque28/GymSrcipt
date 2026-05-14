package gymscript.parser

import gymscript.lexer.Lexer
import org.scalatest.funsuite.AnyFunSuite

final class ParserSpec extends AnyFunSuite {
  private val lexer = new Lexer()

  private def parseSource(source: String): Either[List[ParseError], Program] = {
    val parser = new Parser()
    val tokens = lexer.tokenize(source).toOption.get
    parser.parse(tokens)
  }

  test("parsea declaracion") {
    val result = parseSource("peso meta = 3")

    assert(result.isRight)
    assert(result.toOption.get.statements.head.isInstanceOf[VariableDeclaration])
  }

  test("parsea asignacion") {
    val result = parseSource("peso meta = 3\nmeta = meta + 1")

    assert(result.isRight)
    assert(result.toOption.get.statements(1).isInstanceOf[Assignment])
  }

  test("parsea mostrar") {
    val result = parseSource("mostrar(\"ok\")")

    assert(result.isRight)
    assert(result.toOption.get.statements.head.isInstanceOf[PrintStatement])
  }

  test("parsea if con descanso") {
    val source =
      """si_fuerza verdadero
        |  mostrar("a")
        |descanso
        |  mostrar("b")
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[IfStatement]
    assert(statement.elseBranch.nonEmpty)
  }

  test("parsea if sin descanso") {
    val source =
      """si_fuerza verdadero
        |  mostrar("a")
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[IfStatement]
    assert(statement.elseBranch.isEmpty)
  }

  test("parsea while") {
    val source =
      """peso i = 0
        |mientras_entrenas i menor_que 2
        |  i = i + 1
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    assert(result.toOption.get.statements.exists(_.isInstanceOf[WhileStatement]))
  }

  test("respeta precedencia de operadores") {
    val result = parseSource("mostrar(1 + 2 * 3)")

    assert(result.isRight)
    val statement = result.toOption.get.statements.head.asInstanceOf[PrintStatement]
    val expression = statement.expression.asInstanceOf[BinaryExpression]
    assert(expression.operator == gymscript.lexer.TokenType.Plus)
    assert(expression.right.isInstanceOf[BinaryExpression])
    assert(expression.right.asInstanceOf[BinaryExpression].operator == gymscript.lexer.TokenType.Star)
  }

  test("reporta error por bloque sin fin_rutina") {
    val source =
      """si_fuerza verdadero
        |  mostrar("a")""".stripMargin

    val result = parseSource(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("fin_rutina")))
  }
}
