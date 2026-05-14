package gymscript.resolver

import gymscript.lexer.Lexer
import gymscript.parser.{ ImportDirective, Parser, Program, RoutineDeclaration }
import gymscript.util.{ Position, SourceReader }
import org.scalatest.funsuite.AnyFunSuite

final class ModuleResolverSpec extends AnyFunSuite {
  private val lexer = new Lexer()
  private val parser = new Parser()
  private val resolver = new ModuleResolver(parser)

  private def parseFile(path: String): Program = {
    val source = SourceReader.read(path).toOption.get
    val tokens = lexer.tokenize(source).toOption.get
    parser.parse(tokens).toOption.get
  }

  test("importa archivo valido y combina rutinas") {
    val program = parseFile("examples/modular/main.gym")
    val result = resolver.resolve("examples/modular/main.gym", program, allowLegacySyntax = false)

    assert(result.isRight)
    val routines = result.toOption.get.statements.collect { case routine: RoutineDeclaration => routine.name }
    assert(routines.contains("sumar"))
    assert(routines.contains("motivar"))
    assert(routines.contains("supera_meta"))
  }

  test("rechaza archivo inexistente") {
    val program = Program(Nil, Position.Start, imports = List(ImportDirective("missing.gym", Position.Start)))
    val result = resolver.resolve("examples/module-fixtures/root.gym", program, allowLegacySyntax = false)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.contains("No fue posible leer el modulo")))
  }

  test("rechaza path traversal") {
    val program = Program(Nil, Position.Start, imports = List(ImportDirective("../advanced-routine.gym.txt", Position.Start)))
    val result = resolver.resolve("examples/module-fixtures/root.gym", program, allowLegacySyntax = false)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.contains("directorio seguro")))
  }

  test("detecta import duplicado") {
    val program = parseFile("examples/module-fixtures/duplicate-main.gym")
    val result = resolver.resolve("examples/module-fixtures/duplicate-main.gym", program, allowLegacySyntax = false)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.contains("mas de una vez")))
  }

  test("detecta ciclo de imports") {
    val program = parseFile("examples/module-fixtures/cycle-a.gym")
    val result = resolver.resolve("examples/module-fixtures/cycle-a.gym", program, allowLegacySyntax = false)

    assert(result.isLeft)
    assert(result.swap.toOption.get.exists(_.contains("ciclo de importacion")))
  }
}
