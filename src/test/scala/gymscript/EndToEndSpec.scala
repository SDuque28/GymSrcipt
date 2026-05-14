package gymscript

import gymscript.interpreter.Interpreter
import gymscript.lexer.{ Lexer, LexerOptions }
import gymscript.parser.Parser
import gymscript.resolver.{ ModuleResolver, SemanticAnalyzer }
import gymscript.util.SourceReader
import org.scalatest.funsuite.AnyFunSuite

final class EndToEndSpec extends AnyFunSuite {
  private def runFile(path: String, legacy: Boolean = false): Either[Any, List[String]] = {
    val source = SourceReader.read(path).toOption.get
    val lexer = new Lexer(LexerOptions(allowLegacySyntax = legacy))
    val parser = new Parser()
    val moduleResolver = new ModuleResolver(parser)
    val analyzer = new SemanticAnalyzer()
    val interpreter = new Interpreter()

    for {
      tokens <- lexer.tokenize(source)
      program <- parser.parse(tokens)
      resolved <- moduleResolver.resolve(path, program, allowLegacySyntax = legacy)
      verified <- analyzer.analyze(resolved)
      outputs <- interpreter.execute(verified)
    } yield outputs
  }

  test("basic-routine") {
    assert(runFile("examples/basic-routine.gym.txt") == Right(List("0", "1", "2", "Rutina completada")))
  }

  test("exhaustive-routine") {
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

  test("advanced-routine") {
    assert(runFile("examples/advanced-routine.gym.txt") == Right(List("7", "12", "[press banca, sentadilla, dominadas]", "sentadilla", "3", "[press banca, sentadilla]", "Meta superada", "1", "2")))
  }

  test("modular-main") {
    assert(runFile("examples/modular/main.gym") == Right(List("15", "Vamos con toda, Santiago", "Total listo")))
  }

  test("tail-recursion") {
    assert(runFile("examples/tail-recursion.gym") == Right(List("0")))
  }

  test("legacy-example en modo legacy") {
    assert(runFile("examples/legacy-example.gym.txt", legacy = true) == Right(List("7", "legacy activo")))
  }
}
