package gymscript

import gymscript.interpreter.Interpreter
import gymscript.lexer.Lexer
import gymscript.parser.Parser
import gymscript.resolver.SemanticAnalyzer
import gymscript.util.SourceReader
import org.scalatest.funsuite.AnyFunSuite

final class EndToEndSpec extends AnyFunSuite {
  private val lexer = new Lexer()
  private val parser = new Parser()
  private val analyzer = new SemanticAnalyzer()
  private val interpreter = new Interpreter()

  private def runFile(path: String): Either[Any, List[String]] = {
    val source = SourceReader.read(path).toOption.get
    for {
      tokens <- lexer.tokenize(source)
      program <- parser.parse(tokens)
      verified <- analyzer.analyze(program)
      outputs <- interpreter.execute(verified)
    } yield outputs
  }

  test("ejecuta basic-routine.gym.txt") {
    assert(runFile("examples/basic-routine.gym.txt") == Right(List("0", "1", "2", "Rutina completada")))
  }

  test("ejecuta exhaustive-routine.gym.txt") {
    assert(
      runFile("examples/exhaustive-routine.gym.txt") ==
        Right(
          List(
            "Inicio de rutina",
            "Sentadilla frontal listo",
            "7",
            "9",
            "Serie actual",
            "1",
            "Serie actual",
            "2",
            "Serie actual",
            "3",
            "Rutina avanzada",
            "Condicion verdadera",
            "6",
            "Incremento: 0.5",
            "Fin del entrenamiento"
          )
        )
    )
  }

  test("ejecuta advanced-routine.gym.txt") {
    assert(
      runFile("examples/advanced-routine.gym.txt") ==
        Right(List("[curl, sentadilla, press]", "3", "sentadilla", "Ejercicio: curl", "Alta carga", "Bloque avanzado"))
    )
  }
}
