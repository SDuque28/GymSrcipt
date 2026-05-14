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

  test("rutina retorna valor") {
    val source =
      """rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina
        |mostrar abre_set llamar sumar abre_set 2 separa 3 cierra_set cierra_set""".stripMargin

    assert(runSource(source) == Right(List("5")))
  }

  test("llamada usada en asignacion") {
    val source =
      """rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina
        |peso total cargar llamar sumar abre_set 4 separa 5 cierra_set
        |mostrar abre_set total cierra_set""".stripMargin

    assert(runSource(source) == Right(List("9")))
  }

  test("llamada usada dentro de expresion") {
    val source =
      """rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina
        |mostrar abre_set llamar sumar abre_set 1 separa 2 cierra_set mas_reps 5 cierra_set""".stripMargin

    assert(runSource(source) == Right(List("8")))
  }

  test("subir_peso y bajar_peso con y sin cantidad") {
    val source =
      """peso repeticiones cargar 1
        |subir_peso repeticiones
        |subir_peso repeticiones por 2
        |bajar_peso repeticiones
        |mostrar abre_set repeticiones cierra_set""".stripMargin

    assert(runSource(source) == Right(List("3")))
  }

  test("tomar lista y largo lista") {
    val source =
      """peso ejercicios cargar lista abre_set "curl" separa "press" cierra_set
        |mostrar abre_set tomar abre_set ejercicios separa 0 cierra_set cierra_set
        |mostrar abre_set largo abre_set ejercicios cierra_set cierra_set""".stripMargin

    assert(runSource(source) == Right(List("curl", "2")))
  }

  test("cambiar_set agregar_set quitar_set y rango_set") {
    val source =
      """peso ejercicios cargar lista abre_set "curl" separa "press" cierra_set
        |agregar_set abre_set ejercicios separa "dominadas" cierra_set
        |cambiar_set abre_set ejercicios separa 0 separa "sentadilla" cierra_set
        |quitar_set abre_set ejercicios separa 1 cierra_set
        |peso subset cargar rango_set abre_set ejercicios separa 0 separa 2 cierra_set
        |mostrar abre_set ejercicios cierra_set
        |mostrar abre_set subset cierra_set""".stripMargin

    assert(runSource(source) == Right(List("[sentadilla, dominadas]", "[sentadilla, dominadas]")))
  }

  test("scope local de rutinas") {
    val source =
      """rutina crear abre_set cierra_set entrega numero inicio_rutina
        |  peso interno cargar 1
        |  entregar_resultado interno
        |fin_rutina
        |mostrar abre_set llamar crear abre_set cierra_set cierra_set""".stripMargin

    assert(runSource(source) == Right(List("1")))
  }

  test("division por cero") {
    val result = runSource("mostrar abre_set 10 dividir_rutina 0 cierra_set")

    assert(result.left.toOption.get.asInstanceOf[RuntimeError].message.contains("dividir la rutina entre cero"))
  }

  test("indice fuera de rango") {
    val source =
      """peso ejercicios cargar lista abre_set "curl" cierra_set
        |mostrar abre_set tomar abre_set ejercicios separa 2 cierra_set cierra_set""".stripMargin
    val result = runSource(source)

    assert(result.left.toOption.get.asInstanceOf[RuntimeError].message.contains("fuera de rango"))
  }

  test("recursion basica") {
    val recursiveInterpreter = new Interpreter(maxCallDepth = 32)
    val source =
      """rutina cuenta_regresiva abre_set n como numero cierra_set entrega numero inicio_rutina
        |  si_fuerza n levanta_igual_que 0 inicio_rutina
        |    entregar_resultado 0
        |  descanso inicio_rutina
        |    entregar_resultado llamar cuenta_regresiva abre_set n menos_reps 1 cierra_set
        |  fin_rutina
        |fin_rutina
        |mostrar abre_set llamar cuenta_regresiva abre_set 3 cierra_set cierra_set""".stripMargin

    val result =
      for {
        tokens <- lexer.tokenize(source)
        program <- parser.parse(tokens)
        verified <- analyzer.analyze(program)
        outputs <- recursiveInterpreter.execute(verified)
      } yield outputs

    assert(result == Right(List("0")))
  }

  test("ejecuta advanced-routine.gym.txt") {
    val source = SourceReader.read("examples/advanced-routine.gym.txt").toOption.get
    val result = runSource(source)

    assert(result == Right(List("7", "12", "[press banca, sentadilla, dominadas]", "sentadilla", "3", "[press banca, sentadilla]", "Meta superada", "1", "2")))
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
