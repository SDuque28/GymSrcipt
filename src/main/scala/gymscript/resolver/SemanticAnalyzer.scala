package gymscript.resolver

import gymscript.lexer.TokenType
import gymscript.parser._
import gymscript.util.Position

import scala.collection.mutable

final case class SemanticError(message: String, position: Position) {
  def render: String = s"Error semantico en ${position.render}: $message"
}

final class SemanticAnalyzer {
  def analyze(program: Program): Either[List[SemanticError], Program] = {
    new Analyzer(program).run()
  }

  private sealed trait StaticType {
    def displayName: String
  }

  private object StaticType {
    case object NumberType extends StaticType { val displayName = "numero" }
    case object StringType extends StaticType { val displayName = "string" }
    case object BooleanType extends StaticType { val displayName = "booleano" }
    case object NullType extends StaticType { val displayName = "null" }
    case object UnknownType extends StaticType { val displayName = "desconocido" }
  }

  private final class Analyzer(program: Program) {
    import StaticType._

    private val errors = mutable.ListBuffer.empty[SemanticError]
    private val scopes = mutable.ArrayBuffer(mutable.LinkedHashMap.empty[String, StaticType])

    def run(): Either[List[SemanticError], Program] = {
      analyzeStatements(program.statements)
      if (errors.nonEmpty) Left(errors.toList) else Right(program)
    }

    private def analyzeStatements(statements: List[Statement]): Unit = {
      statements.foreach(analyzeStatement)
    }

    private def analyzeStatement(statement: Statement): Unit = {
      statement match {
        case VariableDeclaration(name, initializer, position) =>
          if (currentScope.contains(name)) {
            error(position, s"La variable '$name' ya fue declarada en este alcance.")
          } else {
            val inferredType = initializer.map(analyzeExpression).getOrElse(UnknownType)
            currentScope.update(name, inferredType)
          }

        case Assignment(name, expression, position) =>
          val expressionType = analyzeExpression(expression)
          resolveScope(name) match {
            case Some(scope) => scope.update(name, expressionType)
            case None => error(position, s"No se puede asignar a '$name' porque no ha sido declarada.")
          }

        case PrintStatement(expression, _) =>
          analyzeExpression(expression)

        case IfStatement(condition, thenBranch, elseBranch, _) =>
          validateBooleanCondition(condition, "si_fuerza")
          withScope(analyzeStatements(thenBranch.statements))
          elseBranch.foreach(block => withScope(analyzeStatements(block.statements)))

        case WhileStatement(condition, body, _) =>
          validateBooleanCondition(condition, "mientras_entrenas")
          withScope(analyzeStatements(body.statements))

        case Block(statements, _) =>
          withScope(analyzeStatements(statements))

        case ExpressionStatement(expression, _) =>
          analyzeExpression(expression)
      }
    }

    private def analyzeExpression(expression: Expression): StaticType = {
      expression match {
        case LiteralExpression(value, _) =>
          value match {
            case NumberLiteral(_) => NumberType
            case StringLiteral(_) => StringType
            case BooleanLiteral(_) => BooleanType
            case NullLiteral => NullType
          }

        case VariableExpression(name, position) =>
          resolveType(name) match {
            case Some(valueType) => valueType
            case None =>
              error(position, s"La variable '$name' no ha sido declarada.")
              UnknownType
          }

        case GroupingExpression(inner, _) =>
          analyzeExpression(inner)

        case UnaryExpression(operator, inner, position) =>
          val innerType = analyzeExpression(inner)
          operator match {
            case TokenType.Minus =>
              requireType(innerType, NumberType, position, "El operador '-' unario requiere un numero.")
              NumberType

            case TokenType.Not =>
              requireType(innerType, BooleanType, position, "El operador 'no' requiere un booleano.")
              BooleanType

            case _ =>
              UnknownType
          }

        case BinaryExpression(left, operator, right, position) =>
          val leftType = analyzeExpression(left)
          val rightType = analyzeExpression(right)
          analyzeBinaryExpression(leftType, operator, rightType, position)
      }
    }

    private def analyzeBinaryExpression(
        leftType: StaticType,
        operator: TokenType,
        rightType: StaticType,
        position: Position
    ): StaticType = {
      import StaticType._

      operator match {
        case TokenType.Plus =>
          (leftType, rightType) match {
            case (NumberType, NumberType) => NumberType
            case (StringType, _) => StringType
            case (_, StringType) => StringType
            case (UnknownType, _) | (_, UnknownType) => UnknownType
            case _ =>
              error(position, "La operacion '+' solo admite numeros o concatenacion con strings.")
              UnknownType
          }

        case TokenType.Minus | TokenType.Star | TokenType.Slash =>
          validateNumericOperands(leftType, rightType, position, operator.lexemeName)
          NumberType

        case TokenType.GreaterThan | TokenType.LessThan | TokenType.GreaterEqual | TokenType.LessEqual =>
          validateNumericOperands(leftType, rightType, position, operator.lexemeName)
          BooleanType

        case TokenType.And | TokenType.Or =>
          validateBooleanOperands(leftType, rightType, position, operator.lexemeName)
          BooleanType

        case TokenType.EqualEqual | TokenType.BangEqual =>
          BooleanType

        case _ =>
          UnknownType
      }
    }

    private def validateBooleanCondition(expression: Expression, context: String): Unit = {
      val expressionType = analyzeExpression(expression)
      if (expressionType != StaticType.BooleanType && expressionType != StaticType.UnknownType) {
        error(expression.position, s"La condicion de '$context' debe ser booleana.")
      }
    }

    private def validateNumericOperands(
        leftType: StaticType,
        rightType: StaticType,
        position: Position,
        operator: String
    ): Unit = {
      if (!isCompatible(leftType, StaticType.NumberType) || !isCompatible(rightType, StaticType.NumberType)) {
        error(position, s"El operador '$operator' requiere operandos numericos.")
      }
    }

    private def validateBooleanOperands(
        leftType: StaticType,
        rightType: StaticType,
        position: Position,
        operator: String
    ): Unit = {
      if (!isCompatible(leftType, StaticType.BooleanType) || !isCompatible(rightType, StaticType.BooleanType)) {
        error(position, s"El operador '$operator' requiere operandos booleanos.")
      }
    }

    private def requireType(actual: StaticType, expected: StaticType, position: Position, message: String): Unit = {
      if (!isCompatible(actual, expected)) {
        error(position, message)
      }
    }

    private def isCompatible(actual: StaticType, expected: StaticType): Boolean = {
      actual == expected || actual == StaticType.UnknownType
    }

    private def withScope(block: => Unit): Unit = {
      scopes.append(mutable.LinkedHashMap.empty[String, StaticType])
      try block
      finally scopes.remove(scopes.length - 1)
    }

    private def currentScope: mutable.LinkedHashMap[String, StaticType] = scopes.last

    private def resolveType(name: String): Option[StaticType] = {
      resolveScope(name).flatMap(_.get(name))
    }

    private def resolveScope(name: String): Option[mutable.LinkedHashMap[String, StaticType]] = {
      scopes.reverseIterator.find(_.contains(name))
    }

    private def error(position: Position, message: String): Unit = {
      errors += SemanticError(message, position)
    }
  }
}
