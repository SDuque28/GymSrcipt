package gymscript.interpreter

import gymscript.parser.{ BooleanLiteral, LiteralExpression, PrintStatement, Program, StringLiteral }
import gymscript.util.Position
import org.scalatest.funsuite.AnyFunSuite

final class InterpreterSpec extends AnyFunSuite {
  test("execute imprime la salida de un programa simple") {
    val interpreter = new Interpreter()
    val statement = PrintStatement(LiteralExpression(StringLiteral("Rutina lista"), Position.Start), Position.Start)
    val program = Program(List(statement), Position.Start)

    val result = interpreter.execute(program)

    assert(result == Right(List("Rutina lista")))
  }

  test("execute evalua booleanos como valores literales") {
    val interpreter = new Interpreter()
    val statement = PrintStatement(LiteralExpression(BooleanLiteral(true), Position.Start), Position.Start)
    val program = Program(List(statement), Position.Start)

    val result = interpreter.execute(program)

    assert(result == Right(List("true")))
  }
}
