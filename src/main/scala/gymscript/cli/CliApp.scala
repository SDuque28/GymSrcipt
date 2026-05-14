package gymscript.cli

import gymscript.interpreter.Interpreter
import gymscript.lexer.{ Lexer, LexerOptions, Token }
import gymscript.parser.Parser
import gymscript.resolver.{ ModuleResolver, SemanticAnalyzer }
import gymscript.util.SourceReader

final class CliApp(
    parser: Parser = new Parser(),
    moduleResolver: ModuleResolver = new ModuleResolver(),
    semanticAnalyzer: SemanticAnalyzer = new SemanticAnalyzer(),
    interpreter: Interpreter = new Interpreter()
) {

  def run(args: Array[String]): Int = {
    parseConfig(args.toList) match {
      case Left(message) =>
        Console.err.println(message)
        1

      case Right(config) if config.showHelp =>
        println(helpText)
        0

      case Right(config) =>
        runWithConfig(config)
    }
  }

  private def runWithConfig(config: CliConfig): Int = {
    val lexer = new Lexer(LexerOptions(allowLegacySyntax = config.allowLegacy))

    SourceReader.read(config.path) match {
      case Left(error) =>
        Console.err.println(error)
        1

      case Right(source) =>
        lexer.tokenize(source) match {
          case Left(errors) =>
            printLines(errors.map(_.render))
            1

          case Right(tokens) =>
            if (config.showTokens) {
              printTokens(tokens)
            }
            parser.parse(tokens) match {
              case Left(errors) =>
                printLines(errors.map(_.render))
                1

              case Right(program) =>
                moduleResolver.resolve(config.path, program, config.allowLegacy) match {
                  case Left(errors) =>
                    printLines(errors)
                    1

                  case Right(resolvedProgram) =>
                    if (config.showAst) {
                      println(resolvedProgram)
                    }
                    semanticAnalyzer.analyze(resolvedProgram) match {
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
  }

  private def printTokens(tokens: List[Token]): Unit = {
    tokens.foreach { token =>
      val literal = token.literal.map(value => s", literal=$value").getOrElse("")
      println(s"${token.tokenType}(${token.lexeme}) @ ${token.position.line}:${token.position.column}$literal")
    }
  }

  private def printLines(lines: Seq[String]): Unit = {
    lines.foreach(Console.err.println)
  }

  private def parseConfig(args: List[String]): Either[String, CliConfig] = {
    val initial = CliConfig()

    args.foldLeft[Either[String, CliConfig]](Right(initial)) {
      case (accEither, arg) =>
        accEither.flatMap { config =>
          arg match {
            case "--legacy" if config.forceStrict =>
              Left("No puedes combinar --legacy con --strict.")
            case "--legacy" =>
              Right(config.copy(allowLegacy = true))
            case "--strict" if config.allowLegacy =>
              Left("No puedes combinar --strict con --legacy.")
            case "--strict" =>
              Right(config.copy(forceStrict = true))
            case "--tokens" =>
              Right(config.copy(showTokens = true))
            case "--ast" =>
              Right(config.copy(showAst = true))
            case "--help" =>
              Right(config.copy(showHelp = true))
            case path if path.startsWith("--") =>
              Left(s"Opcion no reconocida: $path")
            case path if config.path.nonEmpty =>
              Left(s"Se recibieron multiples rutas de archivo. Ya estaba definida: ${config.path}")
            case path =>
              Right(config.copy(path = path))
          }
        }
    }.flatMap { config =>
      if (config.showHelp) Right(config)
      else if (config.path.isEmpty) Left(helpText)
      else Right(config)
    }
  }

  private val helpText: String =
    """Uso:
      |  sbt "run <ruta-del-archivo.gym.txt> [--strict] [--legacy] [--tokens] [--ast]"
      |
      |Opciones:
      |  --strict   Fuerza el modo tematico estricto. Es el modo por defecto.
      |  --legacy   Activa compatibilidad con simbolos y aliases legacy.
      |  --tokens   Imprime los tokens generados por el lexer.
      |  --ast      Imprime el AST generado por el parser.
      |  --help     Muestra esta ayuda.
      |""".stripMargin
}

final case class CliConfig(
    path: String = "",
    allowLegacy: Boolean = false,
    forceStrict: Boolean = false,
    showTokens: Boolean = false,
    showAst: Boolean = false,
    showHelp: Boolean = false
)
