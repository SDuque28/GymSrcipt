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

      case RoutineDeclaration(name, parameters, body, _) =>
        val routine = RoutineValue(name, parameters, body, environment)
        Right(environment.define(name, routine) -> Nil)

      case CallStatement(name, arguments, position) =>
        executeCall(name, arguments, environment, position)

      case block: Block =>
        executeBlock(block, environment)

      case ExpressionStatement(expression, _) =>
        evaluate(expression, environment).map(_ => environment -> Nil)
    }
  }

  private def executeCall(
      name: String,
      arguments: List[Expression],
      environment: Environment,
      position: Position
  ): Either[RuntimeError, (Environment, List[String])] = {
    for {
      callee <- environment.resolve(name, position)
      values <- evaluateAll(arguments, environment)
      result <- callee match {
        case routine: RoutineValue =>
          callRoutine(routine, values, position).map(outputs => environment -> outputs)
        case _ =>
          Left(RuntimeError(s"'$name' no es una rutina invocable.", position))
      }
    } yield result
  }

  private def callRoutine(
      routine: RoutineValue,
      arguments: List[Value],
      position: Position
  ): Either[RuntimeError, List[String]] = {
    if (routine.parameters.length != arguments.length) {
      Left(
        RuntimeError(
          s"La rutina '${routine.name}' esperaba ${routine.parameters.length} argumento(s) y recibio ${arguments.length}.",
          position
        )
      )
    } else {
      val localEnvironment = routine.parameters.zip(arguments).foldLeft(routine.closure.child) {
        case (env, (parameter, argument)) => env.define(parameter, argument)
      }
      executeStatements(routine.body.statements, localEnvironment).map(_._2)
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

      case ListExpression(elements, _) =>
        evaluateAll(elements, environment).map(values => ListValue(values.toVector))

      case TakeExpression(collection, indexExpression, position) =>
        for {
          collectionValue <- evaluate(collection, environment)
          indexValue <- evaluate(indexExpression, environment)
          index <- asWholeNumber(indexValue, position)
          result <- takeFromList(collectionValue, index, position)
        } yield result

      case LengthExpression(collection, position) =>
        evaluate(collection, environment).flatMap(lengthOfList(_, position))

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

  private def evaluateAll(expressions: List[Expression], environment: Environment): Either[RuntimeError, List[Value]] = {
    expressions.foldLeft[Either[RuntimeError, List[Value]]](Right(Nil)) {
      case (accEither, expression) =>
        for {
          acc <- accEither
          value <- evaluate(expression, environment)
        } yield acc :+ value
    }
  }

  private def applyUnary(operator: TokenType, value: Value, position: Position): Either[RuntimeError, Value] = {
    operator match {
      case TokenType.Minus =>
        value match {
          case NumberValue(number) => Right(NumberValue(-number))
          case _ => Left(RuntimeError("El operador 'menos_reps' requiere un numero.", position))
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
          case _ => Left(RuntimeError("El operador 'mas_reps' requiere numeros o strings.", position))
        }

      case TokenType.Minus =>
        numericBinary(left, right, position, "menos_reps", _ - _)

      case TokenType.Star =>
        numericBinary(left, right, position, "series_de", _ * _)

      case TokenType.Slash =>
        (left, right) match {
          case (_, NumberValue(divisor)) if divisor == 0 =>
            Left(RuntimeError("No se puede dividir la rutina entre cero.", position))
          case _ =>
            numericBinary(left, right, position, "dividir_rutina", _ / _)
        }

      case TokenType.EqualEqual =>
        Right(BooleanValue(left == right))

      case TokenType.BangEqual =>
        Right(BooleanValue(left != right))

      case TokenType.GreaterThan =>
        numericComparison(left, right, position, "levanta_mas_que", _ > _)

      case TokenType.LessThan =>
        numericComparison(left, right, position, "levanta_menos_que", _ < _)

      case TokenType.GreaterEqual =>
        numericComparison(left, right, position, "levanta_minimo", _ >= _)

      case TokenType.LessEqual =>
        numericComparison(left, right, position, "levanta_maximo", _ <= _)

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
      operatorName: String,
      operation: (BigDecimal, BigDecimal) => BigDecimal
  ): Either[RuntimeError, Value] = {
    (left, right) match {
      case (NumberValue(a), NumberValue(b)) => Right(NumberValue(operation(a, b)))
      case _ => Left(RuntimeError(s"La operacion '$operatorName' requiere dos numeros.", position))
    }
  }

  private def numericComparison(
      left: Value,
      right: Value,
      position: Position,
      operatorName: String,
      operation: (BigDecimal, BigDecimal) => Boolean
  ): Either[RuntimeError, Value] = {
    (left, right) match {
      case (NumberValue(a), NumberValue(b)) => Right(BooleanValue(operation(a, b)))
      case _ => Left(RuntimeError(s"La comparacion '$operatorName' requiere dos numeros.", position))
    }
  }

  private def asBoolean(value: Value, position: Position): Either[RuntimeError, Boolean] = {
    value match {
      case BooleanValue(boolean) => Right(boolean)
      case _ => Left(RuntimeError("La condicion del entrenamiento debe evaluarse a verdadero o falso.", position))
    }
  }

  private def asWholeNumber(value: Value, position: Position): Either[RuntimeError, Int] = {
    value match {
      case NumberValue(number) if number.isValidInt && number == BigDecimal(number.toInt) =>
        Right(number.toInt)
      case _ =>
        Left(RuntimeError("La posicion de 'tomar' debe ser un numero entero.", position))
    }
  }

  private def takeFromList(value: Value, index: Int, position: Position): Either[RuntimeError, Value] = {
    value match {
      case ListValue(values) =>
        values.lift(index).toRight(RuntimeError(s"La posicion $index esta fuera de la rutina de lista.", position))
      case _ =>
        Left(RuntimeError("La operacion 'tomar' solo funciona sobre listas.", position))
    }
  }

  private def lengthOfList(value: Value, position: Position): Either[RuntimeError, Value] = {
    value match {
      case ListValue(values) => Right(NumberValue(values.length))
      case _ => Left(RuntimeError("La operacion 'largo' solo funciona sobre listas.", position))
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
