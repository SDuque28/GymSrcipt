package gymscript.parser

import gymscript.lexer.TokenType
import gymscript.util.Position

final case class Program(statements: List[Statement], position: Position)

sealed trait Statement {
  def position: Position
}

final case class VariableDeclaration(name: String, initializer: Option[Expression], position: Position) extends Statement
final case class Assignment(name: String, expression: Expression, position: Position) extends Statement
final case class PrintStatement(expression: Expression, position: Position) extends Statement
final case class IfStatement(
    condition: Expression,
    thenBranch: Block,
    elseBranch: Option[Block],
    position: Position
) extends Statement
final case class WhileStatement(condition: Expression, body: Block, position: Position) extends Statement
final case class RoutineDeclaration(name: String, parameters: List[String], body: Block, position: Position) extends Statement
final case class CallStatement(name: String, arguments: List[Expression], position: Position) extends Statement
final case class Block(statements: List[Statement], position: Position) extends Statement
final case class ExpressionStatement(expression: Expression, position: Position) extends Statement

sealed trait Expression {
  def position: Position
}

final case class BinaryExpression(
    left: Expression,
    operator: TokenType,
    right: Expression,
    position: Position
) extends Expression
final case class UnaryExpression(operator: TokenType, expression: Expression, position: Position) extends Expression
final case class LiteralExpression(value: LiteralValue, position: Position) extends Expression
final case class VariableExpression(name: String, position: Position) extends Expression
final case class GroupingExpression(expression: Expression, position: Position) extends Expression
final case class ListExpression(elements: List[Expression], position: Position) extends Expression
final case class TakeExpression(collection: Expression, index: Expression, position: Position) extends Expression
final case class LengthExpression(collection: Expression, position: Position) extends Expression

sealed trait LiteralValue
final case class NumberLiteral(value: BigDecimal) extends LiteralValue
final case class StringLiteral(value: String) extends LiteralValue
final case class BooleanLiteral(value: Boolean) extends LiteralValue
case object NullLiteral extends LiteralValue
