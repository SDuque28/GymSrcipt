package gymscript.interpreter

import gymscript.lexer.Lexer
import gymscript.parser._
import gymscript.resolver.SemanticAnalyzer
import gymscript.util.Position
import gymscript.util.SourceReader
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

  test("ejecuta basic-routine.gym.txt") {
    val source = SourceReader.read("examples/basic-routine.gym.txt").toOption.get
    val result = runSource(source)

    assert(result == Right(List("0", "1", "2", "Rutina completada")))
  }

  test("ejecuta operadores tematicos") {
    val result = runSource(
      "mostrar abre_set 1 mas_reps 2 series_de 3 cierra_set\n" +
        "mostrar abre_set abre_set 1 mas_reps 2 cierra_set series_de 3 cierra_set"
    )

    assert(result == Right(List("7", "9")))
  }

  test("ejecuta if else tematico") {
    val source =
      """si_fuerza verdadero inicio_rutina
        |  mostrar abre_set "ok" cierra_set
        |descanso inicio_rutina
        |  mostrar abre_set "no" cierra_set
        |fin_rutina""".stripMargin

    val result = runSource(source)

    assert(result == Right(List("ok")))
  }

  test("ejecuta while tematico") {
    val source =
      """peso i cargar 0
        |mientras_entrenas i levanta_menos_que 3 inicio_rutina
        |  mostrar abre_set i cierra_set
        |  i cargar i mas_reps 1
        |fin_rutina""".stripMargin

    val result = runSource(source)

    assert(result == Right(List("0", "1", "2")))
  }

  test("ejecuta listas y funciones") {
    val source = SourceReader.read("examples/advanced-routine.gym.txt").toOption.get
    val result = runSource(source)

    assert(result == Right(List("[curl, sentadilla, press]", "3", "sentadilla", "Ejercicio: curl", "Alta carga", "Bloque avanzado")))
  }

  test("reporta division por cero con mensaje claro") {
    val source = "mostrar abre_set 10 dividir_rutina 0 cierra_set"
    val result = runSource(source)

    assert(result.left.toOption.get.asInstanceOf[RuntimeError].message.contains("dividir la rutina entre cero"))
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

    assert(result.left.toOption.get.message.contains("series_de"))
  }
}
