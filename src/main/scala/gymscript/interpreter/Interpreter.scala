package gymscript.interpreter

import gymscript.lexer.TokenType
import gymscript.parser._
import gymscript.util.Position

final class Interpreter {
  def execute(program: Program): Either[RuntimeError, List[String]] = {
    executeStatements(program.statements, new Environment()).map(_._2)
  }

  private def executeStatements(
      statements: List[Statement],
      environment: Environment
  ): Either[RuntimeError, (Environment, List[String])] = {
    statements.foldLeft[Either[RuntimeError, (Environment, List[String])]](Right(environment -> Nil)) {
      case (accEither, statement) =>
        accEither.flatMap {
          case (currentEnvironment, outputs) =>
            executeStatement(statement, currentEnvironment).map {
              case (nextEnvironment, newOutputs) =>
                nextEnvironment -> (outputs ++ newOutputs)
            }
        }
    }
  }

  private def executeStatement(
      statement: Statement,
      environment: Environment
  ): Either[RuntimeError, (Environment, List[String])] = {
    statement match {
      case VariableDeclaration(name, initializer, _) =>
        val valueEither = initializer match {
          case Some(expression) => evaluate(expression, environment)
          case None => Right(NullValue: Value)
        }
        valueEither.map(value => environment.define(name, value) -> Nil)

      case Assignment(name, expression, position) =>
        evaluate(expression, environment).flatMap(value => environment.assign(name, value, position).map(_ -> Nil))

      case PrintStatement(expression, _) =>
        evaluate(expression, environment).map(value => environment -> List(value.render))

      case IfStatement(condition, thenBranch, elseBranch, _) =>
        evaluate(condition, environment).flatMap { value =>
          asBoolean(value, condition.position).flatMap { boolean =>
            if (boolean) executeBlock(thenBranch, environment)
            else elseBranch.map(executeBlock(_, environment)).getOrElse(Right(environment -> Nil))
          }
        }

      case WhileStatement(condition, body, _) =>
        executeWhile(condition, body, environment, Nil)

      case block: Block =>
        executeBlock(block, environment)

      case ExpressionStatement(expression, _) =>
        evaluate(expression, environment).map(_ => environment -> Nil)
    }
  }

  private def executeBlock(block: Block, environment: Environment): Either[RuntimeError, (Environment, List[String])] = {
    executeStatements(block.statements, environment.child).map {
      case (_, outputs) => environment -> outputs
    }
  }

  private def executeWhile(
      condition: Expression,
      body: Block,
      environment: Environment,
      outputs: List[String]
  ): Either[RuntimeError, (Environment, List[String])] = {
    evaluate(condition, environment).flatMap { value =>
      asBoolean(value, condition.position).flatMap { boolean =>
        if (!boolean) {
          Right(environment -> outputs)
        } else {
          executeBlock(body, environment).flatMap {
            case (nextEnvironment, loopOutputs) =>
              executeWhile(condition, body, nextEnvironment, outputs ++ loopOutputs)
          }
        }
      }
    }
  }

  private def evaluate(expression: Expression, environment: Environment): Either[RuntimeError, Value] = {
    expression match {
      case LiteralExpression(value, _) =>
        Right(toRuntimeValue(value))

      case VariableExpression(name, position) =>
        environment.resolve(name, position)

      case GroupingExpression(inner, _) =>
        evaluate(inner, environment)

      case UnaryExpression(operator, inner, position) =>
        evaluate(inner, environment).flatMap(value => applyUnary(operator, value, position))

      case BinaryExpression(left, operator, right, position) =>
        for {
          leftValue <- evaluate(left, environment)
          rightValue <- evaluate(right, environment)
          result <- applyBinary(leftValue, operator, rightValue, position)
        } yield result
    }
  }

  private def applyUnary(operator: TokenType, value: Value, position: Position): Either[RuntimeError, Value] = {
    operator match {
      case TokenType.Minus =>
        value match {
          case NumberValue(number) => Right(NumberValue(-number))
          case _ => Left(RuntimeError("El operador '-' unario requiere un numero.", position))
        }

      case TokenType.Not =>
        asBoolean(value, position).map(boolean => BooleanValue(!boolean))

      case _ =>
        Left(RuntimeError(s"Operador unario no soportado: ${operator.lexemeName}.", position))
    }
  }

  private def applyBinary(
      left: Value,
      operator: TokenType,
      right: Value,
      position: Position
  ): Either[RuntimeError, Value] = {
    operator match {
      case TokenType.Plus =>
        (left, right) match {
          case (NumberValue(a), NumberValue(b)) => Right(NumberValue(a + b))
          case (StringValue(a), StringValue(b)) => Right(StringValue(a + b))
          case (StringValue(a), value) => Right(StringValue(a + value.render))
          case (value, StringValue(b)) => Right(StringValue(value.render + b))
          case _ => Left(RuntimeError("El operador '+' requiere numeros o strings.", position))
        }

      case TokenType.Minus =>
        numericBinary(left, right, position, _ - _)

      case TokenType.Star =>
        numericBinary(left, right, position, _ * _)

      case TokenType.Slash =>
        (left, right) match {
          case (_, NumberValue(divisor)) if divisor == 0 =>
            Left(RuntimeError("Division por cero.", position))
          case _ =>
            numericBinary(left, right, position, _ / _)
        }

      case TokenType.EqualEqual =>
        Right(BooleanValue(left == right))

      case TokenType.BangEqual =>
        Right(BooleanValue(left != right))

      case TokenType.GreaterThan =>
        numericComparison(left, right, position, _ > _)

      case TokenType.LessThan =>
        numericComparison(left, right, position, _ < _)

      case TokenType.GreaterEqual =>
        numericComparison(left, right, position, _ >= _)

      case TokenType.LessEqual =>
        numericComparison(left, right, position, _ <= _)

      case TokenType.And =>
        for {
          leftBoolean <- asBoolean(left, position)
          rightBoolean <- asBoolean(right, position)
        } yield BooleanValue(leftBoolean && rightBoolean)

      case TokenType.Or =>
        for {
          leftBoolean <- asBoolean(left, position)
          rightBoolean <- asBoolean(right, position)
        } yield BooleanValue(leftBoolean || rightBoolean)

      case _ =>
        Left(RuntimeError(s"Operador binario no soportado: ${operator.lexemeName}.", position))
    }
  }

  private def numericBinary(
      left: Value,
      right: Value,
      position: Position,
      operation: (BigDecimal, BigDecimal) => BigDecimal
  ): Either[RuntimeError, Value] = {
    (left, right) match {
      case (NumberValue(a), NumberValue(b)) => Right(NumberValue(operation(a, b)))
      case _ => Left(RuntimeError("La operacion requiere dos numeros.", position))
    }
  }

  private def numericComparison(
      left: Value,
      right: Value,
      position: Position,
      operation: (BigDecimal, BigDecimal) => Boolean
  ): Either[RuntimeError, Value] = {
    (left, right) match {
      case (NumberValue(a), NumberValue(b)) => Right(BooleanValue(operation(a, b)))
      case _ => Left(RuntimeError("La comparacion requiere dos numeros.", position))
    }
  }

  private def asBoolean(value: Value, position: Position): Either[RuntimeError, Boolean] = {
    value match {
      case BooleanValue(boolean) => Right(boolean)
      case _ => Left(RuntimeError("Se esperaba un valor booleano.", position))
    }
  }

  private def toRuntimeValue(value: LiteralValue): Value = {
    value match {
      case NumberLiteral(number) => NumberValue(number)
      case StringLiteral(text) => StringValue(text)
      case BooleanLiteral(boolean) => BooleanValue(boolean)
      case NullLiteral => NullValue
    }
  }
}
