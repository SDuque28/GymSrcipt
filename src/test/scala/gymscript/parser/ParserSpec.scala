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

  test("parsea parametros con como y retorno con entrega") {
    val source =
      """rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    val routine = result.toOption.get.statements.head.asInstanceOf[RoutineDeclaration]
    assert(routine.parameters.map(_.name) == List("a", "b"))
    assert(routine.parameters.forall(_.typeAnnotation.nonEmpty))
    assert(routine.returnType.contains(SimpleTypeAnnotation("numero", routine.returnType.get.position)))
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

  test("parsea importar_rutina al inicio") {
    val source =
      """importar_rutina "math.gym"
        |rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    assert(result.toOption.get.imports.map(_.path) == List("math.gym"))
  }

  test("parsea tipos lista_de") {
    val source =
      """rutina primeros abre_set ejercicios como lista_de texto cierra_set entrega lista_de texto inicio_rutina
        |  entregar_resultado rango_set abre_set ejercicios separa 0 separa 1 cierra_set
        |fin_rutina""".stripMargin

    val result = parseSource(source)

    assert(result.isRight)
    val routine = result.toOption.get.statements.head.asInstanceOf[RoutineDeclaration]
    assert(routine.parameters.head.typeAnnotation.exists(_.isInstanceOf[ListTypeAnnotation]))
    assert(routine.returnType.exists(_.isInstanceOf[ListTypeAnnotation]))
  }

  test("reporta error si falta inicio_rutina en rutina") {
    val result = parseSource("rutina sumar abre_set a como numero cierra_set entrega numero entregar_resultado a")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("inicio_rutina")))
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
