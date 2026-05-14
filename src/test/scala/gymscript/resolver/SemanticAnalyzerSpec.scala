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

  test("valida condicion booleana en si_fuerza") {
    val result = analyze("si_fuerza 3 inicio_rutina\n  mostrar abre_set \"x\" cierra_set\nfin_rutina")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("booleana")))
  }

  test("valida retorno declarado numero") {
    val source =
      """rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina""".stripMargin

    assert(analyze(source).isRight)
  }

  test("detecta retorno inconsistente") {
    val source =
      """rutina raro abre_set valor como numero cierra_set entrega numero inicio_rutina
        |  si_fuerza valor levanta_mas_que 0 inicio_rutina
        |    entregar_resultado valor
        |  descanso inicio_rutina
        |    entregar_resultado "texto"
        |  fin_rutina
        |fin_rutina""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("declara retorno numero")))
  }

  test("detecta rutina que declara retorno pero no retorna") {
    val source =
      """rutina imprimir abre_set mensaje como texto cierra_set entrega texto inicio_rutina
        |  mostrar abre_set mensaje cierra_set
        |fin_rutina""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("no entrega ningun resultado")))
  }

  test("valida tipos de argumentos") {
    val source =
      """rutina saludar abre_set nombre como texto cierra_set entrega texto inicio_rutina
        |  entregar_resultado "Hola " mas_reps nombre
        |fin_rutina
        |peso total cargar llamar saludar abre_set 1 cierra_set""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("espera texto")))
  }

  test("valida tipo de retorno usado en condicion") {
    val source =
      """rutina sumar abre_set a como numero separa b como numero cierra_set entrega numero inicio_rutina
        |  entregar_resultado a mas_reps b
        |fin_rutina
        |si_fuerza llamar sumar abre_set 1 separa 2 cierra_set inicio_rutina
        |  mostrar abre_set "ok" cierra_set
        |fin_rutina""".stripMargin

    val result = analyze(source)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("condicion de 'si_fuerza' debe ser booleana")))
  }

  test("detecta lista con tipos incompatibles") {
    val result = analyze("""peso ejercicios cargar lista abre_set "curl" separa 10 cierra_set""")

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.message.contains("mezcla tipos incompatibles")))
  }

  test("valida tomar largo rango_set agregar_set y cambiar_set") {
    val source =
      """peso ejercicios cargar lista abre_set "curl" separa "press" cierra_set
        |agregar_set abre_set ejercicios separa "dominadas" cierra_set
        |cambiar_set abre_set ejercicios separa 0 separa "sentadilla" cierra_set
        |peso primero cargar tomar abre_set ejercicios separa 0 cierra_set
        |peso tamano cargar largo abre_set ejercicios cierra_set
        |peso subset cargar rango_set abre_set ejercicios separa 0 separa 2 cierra_set
        |mostrar abre_set primero mas_reps tamano cierra_set
        |mostrar abre_set subset cierra_set""".stripMargin

    assert(analyze(source).isRight)
  }
}
