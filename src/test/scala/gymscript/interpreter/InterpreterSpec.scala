package gymscript.interpreter

import gymscript.lexer.Lexer
import gymscript.parser._
import gymscript.resolver.SemanticAnalyzer
import gymscript.util.SourceReader
import gymscript.util.Position
import org.scalatest.funsuite.AnyFunSuite

final class InterpreterSpec extends AnyFunSuite {
  private val lexer = new Lexer()
  private val parser = new Parser()
  private val analyzer = new SemanticAnalyzer()
  private val interpreter = new Interpreter()

  private def runSource(source: String): Either[Any, List[String]] = {
    lexer.tokenize(source) match {
      case Left(errors) => Left(errors)
      case Right(tokens) =>
        parser.parse(tokens) match {
          case Left(errors) => Left(errors)
          case Right(program) =>
            analyzer.analyze(program) match {
              case Left(errors) => Left(errors)
              case Right(validProgram) => interpreter.execute(validProgram)
            }
        }
    }
  }

  test("execute imprime la salida de un programa simple") {
    val statement = PrintStatement(LiteralExpression(StringLiteral("Rutina lista"), Position.Start), Position.Start)
    val program = Program(List(statement), Position.Start)

    val result = interpreter.execute(program)

    assert(result == Right(List("Rutina lista")))
  }

  test("execute evalua booleanos como valores literales") {
    val statement = PrintStatement(LiteralExpression(BooleanLiteral(true), Position.Start), Position.Start)
    val program = Program(List(statement), Position.Start)

    val result = interpreter.execute(program)

    assert(result == Right(List("true")))
  }

  test("ejecuta basic-routine.gym.txt") {
    val source = SourceReader.read("examples/basic-routine.gym.txt").toOption.get
    val result = runSource(source)

    assert(result == Right(List("0", "1", "2", "Rutina completada")))
  }

  test("ejecuta operaciones aritmeticas") {
    val result = runSource("mostrar(1 + 2 * 3)\nmostrar((1 + 2) * 3)")

    assert(result == Right(List("7", "9")))
  }

  test("ejecuta if else") {
    val source =
      """si_fuerza verdadero
        |  mostrar("ok")
        |descanso
        |  mostrar("no")
        |fin_rutina""".stripMargin

    val result = runSource(source)

    assert(result == Right(List("ok")))
  }

  test("ejecuta while") {
    val source =
      """peso i = 0
        |mientras_entrenas i menor_que 3
        |  mostrar(i)
        |  i = i + 1
        |fin_rutina""".stripMargin

    val result = runSource(source)

    assert(result == Right(List("0", "1", "2")))
  }

  test("reporta division por cero") {
    val program = Program(
      List(
        PrintStatement(
          BinaryExpression(
            LiteralExpression(NumberLiteral(10), Position.Start),
            gymscript.lexer.TokenType.Slash,
            LiteralExpression(NumberLiteral(0), Position.Start),
            Position.Start
          ),
          Position.Start
        )
      ),
      Position.Start
    )

    val result = interpreter.execute(program)

    assert(result.left.toOption.get.message.contains("Division por cero"))
  }

  test("reporta operacion incompatible") {
    val program = Program(
      List(
        PrintStatement(
          BinaryExpression(
            LiteralExpression(BooleanLiteral(true), Position.Start),
            gymscript.lexer.TokenType.Star,
            LiteralExpression(NumberLiteral(2), Position.Start),
            Position.Start
          ),
          Position.Start
        )
      ),
      Position.Start
    )

    val result = interpreter.execute(program)

    assert(result.left.toOption.get.message.contains("dos numeros"))
  }
}
