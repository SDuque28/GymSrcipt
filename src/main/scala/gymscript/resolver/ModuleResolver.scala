package gymscript.resolver

import gymscript.lexer.{ Lexer, LexerOptions }
import gymscript.parser.{ ImportDirective, ParseError, Parser, Program, RoutineDeclaration, Statement }
import gymscript.util.{ Position, SourceReader }

import java.nio.file.{ Path, Paths }
import scala.collection.mutable

final class ModuleResolver(parser: Parser = new Parser()) {
  def resolve(entryPath: String, entryProgram: Program, allowLegacySyntax: Boolean): Either[List[String], Program] = {
    val errors = mutable.ListBuffer.empty[String]
    val rootPath = Paths.get(entryPath).toAbsolutePath.normalize()
    val rootDirectory = Option(rootPath.getParent).getOrElse(rootPath)
    val visited = mutable.LinkedHashSet.empty[Path]
    val visiting = mutable.ArrayBuffer.empty[Path]
    val moduleRoutines = mutable.LinkedHashMap.empty[Path, List[RoutineDeclaration]]

    def visitImports(imports: List[ImportDirective], currentFile: Path, entry: Boolean): Unit = {
      val importedTargets = mutable.LinkedHashSet.empty[Path]

      imports.foreach { importDirective =>
        resolveImportPath(importDirective, currentFile, rootDirectory) match {
          case Left(message) =>
            errors += message

          case Right(target) if importedTargets.contains(target) =>
            errors += moduleError(
              s"El archivo '${currentFile.getFileName}' importa '${importDirective.path}' mas de una vez.",
              importDirective.position,
              Some(importDirective.path)
            )

          case Right(target) =>
            importedTargets += target
            if (visiting.contains(target)) {
              val cycle = (visiting.toList :+ target).map(_.getFileName.toString).mkString(" -> ")
              errors += moduleError(
                s"Se detecto un ciclo de importacion: $cycle.",
                importDirective.position,
                Some(importDirective.path)
              )
            } else if (!visited.contains(target)) {
              readProgram(target, allowLegacySyntax) match {
                case Left(readErrors) =>
                  errors ++= readErrors

                case Right(program) =>
                  if (!entry) {
                    validateImportedStatements(program.statements, target).foreach(errors += _)
                  }

                  visiting += target
                  visitImports(program.imports, target, entry = false)
                  visiting.remove(visiting.length - 1)
                  visited += target
                  moduleRoutines.update(target, collectRoutines(program.statements))
              }
            }
        }
      }
    }

    visiting += rootPath
    visitImports(entryProgram.imports, rootPath, entry = true)
    visiting.remove(visiting.length - 1)

    if (errors.nonEmpty) {
      Left(errors.toList)
    } else {
      val importedStatements = moduleRoutines.valuesIterator.flatten.toList
      Right(Program(importedStatements ++ entryProgram.statements, entryProgram.position, Nil))
    }
  }

  private def collectRoutines(statements: List[Statement]): List[RoutineDeclaration] = {
    statements.collect { case declaration: RoutineDeclaration => declaration }
  }

  private def validateImportedStatements(statements: List[Statement], modulePath: Path): List[String] = {
    statements.collect {
      case statement if !statement.isInstanceOf[RoutineDeclaration] =>
        moduleError(
          s"El modulo '${modulePath.getFileName}' solo puede exportar rutinas en el nivel superior.",
          statement.position,
          Some(modulePath.toString)
        )
    }
  }

  private def readProgram(path: Path, allowLegacySyntax: Boolean): Either[List[String], Program] = {
    SourceReader.read(path.toString) match {
      case Left(error) =>
        Left(List(moduleError(s"No fue posible leer el modulo '${path.getFileName}': $error", Position.Start, Some(path.toString))))

      case Right(source) =>
        val lexer = new Lexer(LexerOptions(allowLegacySyntax = allowLegacySyntax))
        lexer.tokenize(source) match {
          case Left(errors) => Left(errors.map(_.render))
          case Right(tokens) =>
            parser.parse(tokens) match {
              case Left(errors) => Left(errors.map(_.render))
              case Right(program) => Right(program)
            }
        }
    }
  }

  private def resolveImportPath(importDirective: ImportDirective, currentFile: Path, rootDirectory: Path): Either[String, Path] = {
    val rawPath = importDirective.path
    val currentDirectory = Option(currentFile.getParent).getOrElse(rootDirectory)
    val unresolved = Paths.get(rawPath)

    if (unresolved.isAbsolute) {
      Left(moduleError("Los imports de GymScript deben usar rutas relativas.", importDirective.position, Some(rawPath)))
    } else {
      val normalizedTarget = currentDirectory.resolve(unresolved).normalize()
      if (!normalizedTarget.startsWith(rootDirectory)) {
        Left(
          moduleError(
            s"Se bloqueo la ruta '$rawPath' porque intenta salir del directorio seguro de imports.",
            importDirective.position,
            Some(rawPath)
          )
        )
      } else if (!normalizedTarget.toString.endsWith(".gym") && !normalizedTarget.toString.endsWith(".gym.txt")) {
        Left(
          moduleError(
            s"El import '$rawPath' debe apuntar a un archivo '.gym' o '.gym.txt'.",
            importDirective.position,
            Some(rawPath)
          )
        )
      } else {
        Right(normalizedTarget.toAbsolutePath.normalize())
      }
    }
  }

  private def moduleError(message: String, position: Position, context: Option[String]): String = {
    val contextDetail = context.map(value => s" Context: '$value'.").getOrElse("")
    s"[SEMANTIC ERROR] line ${position.line}, column ${position.column}: $message$contextDetail"
  }
}
