package gymscript.cli

import gymscript.interpreter.Interpreter
import gymscript.lexer.Lexer
import gymscript.parser.Parser
import gymscript.resolver.SemanticAnalyzer
import gymscript.util.SourceReader

final class CliApp(
    lexer: Lexer = new Lexer(),
    parser: Parser = new Parser(),
    semanticAnalyzer: SemanticAnalyzer = new SemanticAnalyzer(),
    interpreter: Interpreter = new Interpreter()
) {

  def run(args: Array[String]): Int = {
    if (args.length != 1) {
      Console.err.println("Uso: sbt \"run <ruta-del-archivo.gym.txt>\"")
      return 1
    }

    val path = args(0)

    SourceReader.read(path) match {
      case Left(error) =>
        Console.err.println(error)
        1

      case Right(source) =>
        lexer.tokenize(source) match {
          case Left(errors) =>
            printLines(errors.map(_.render))
            1

          case Right(tokens) =>
            parser.parse(tokens) match {
              case Left(errors) =>
                printLines(errors.map(_.render))
                1

              case Right(program) =>
                semanticAnalyzer.analyze(program) match {
                  case Left(errors) =>
                    printLines(errors.map(_.render))
                    1

                  case Right(validProgram) =>
                    interpreter.execute(validProgram) match {
                      case Left(error) =>
                        Console.err.println(error.render)
                        1

                      case Right(outputs) =>
                        outputs.foreach(println)
                        0
                    }
                }
            }
        }
    }
  }

  private def printLines(lines: Seq[String]): Unit = {
    lines.foreach(Console.err.println)
  }
}
