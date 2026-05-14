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

  test("parsea rutina con retorno") {
    val source =
      """rutina sumar abre_set a separa b cierra_set inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    val routine = result.toOption.get.statements.head.asInstanceOf[RoutineDeclaration]
    assert(routine.body.statements.head.isInstanceOf[ReturnStatement])
  }

  test("parsea llamada como expresion") {
    val result = parseSource("peso total cargar llamar sumar abre_set 2 separa 3 cierra_set")

    assert(result.isRight)
    val declaration = result.toOption.get.statements.head.asInstanceOf[VariableDeclaration]
    assert(declaration.initializer.get.isInstanceOf[CallExpression])
  }

  test("parsea llamada anidada") {
    val result = parseSource("peso total cargar llamar sumar abre_set 1 separa llamar sumar abre_set 2 separa 3 cierra_set cierra_set")

    assert(result.isRight)
  }

  test("parsea subir_peso y bajar_peso") {
    val source =
      """peso repeticiones cargar 0
        |subir_peso repeticiones
        |bajar_peso repeticiones por 2""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    assert(result.toOption.get.statements(1).isInstanceOf[AdjustWeightStatement])
    assert(result.toOption.get.statements(2).isInstanceOf[AdjustWeightStatement])
  }

  test("parsea operaciones de lista") {
    val source =
      """peso ejercicios cargar lista abre_set "curl" separa "press" cierra_set
        |cambiar_set abre_set ejercicios separa 0 separa "sentadilla" cierra_set
        |agregar_set abre_set ejercicios separa "dominadas" cierra_set
        |quitar_set abre_set ejercicios separa 1 cierra_set
        |peso subset cargar rango_set abre_set ejercicios separa 0 separa 1 cierra_set""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    assert(result.toOption.get.statements.exists(_.isInstanceOf[ChangeSetStatement]))
    assert(result.toOption.get.statements.exists(_.isInstanceOf[AddSetStatement]))
    assert(result.toOption.get.statements.exists(_.isInstanceOf[RemoveSetStatement]))
  }

  test("reporta error por argumentos faltantes") {
    val result = parseSource("llamar sumar abre_set 1 separa cierra_set")

    assert(result.isLeft)
  }

  test("reporta error por bloque mal cerrado") {
    val source =
      """si_fuerza verdadero inicio_rutina
        |  mostrar abre_set "ok" cierra_set""".stripMargin

    val result = parseSource(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("fin_rutina")))
  }
}
