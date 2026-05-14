package gymscript.interpreter

import gymscript.lexer.TokenType
import gymscript.parser._
import gymscript.util.Position

import scala.collection.mutable.ListBuffer

final class Interpreter(
    maxCallDepth: Int = 256,
    maxLoopIterations: Int = 10000
) {
  def execute(program: Program): Either[RuntimeError, List[String]] = {
    val context = new ExecutionContext
    executeStatements(program.statements, new Environment(), context, insideRoutine = false)
      .map(_ => context.outputs.toList)
  }

  private final class ExecutionContext {
    val outputs: ListBuffer[String] = ListBuffer.empty
    var callDepth: Int = 0
  }

  private sealed trait ControlFlow
  private case object ContinueFlow extends ControlFlow
  private final class ReturnFlow(val value: Value) extends ControlFlow

  private def executeStatements(
      statements: List[Statement],
      environment: Environment,
      context: ExecutionContext,
      insideRoutine: Boolean
  ): Either[RuntimeError, ControlFlow] = {
    var index = 0
    while (index < statements.length) {
      executeStatement(statements(index), environment, context, insideRoutine) match {
        case Left(error) => return Left(error)
        case Right(flow: ReturnFlow) => return Right(flow)
        case Right(ContinueFlow) =>
          index += 1
      }
    }
    Right(ContinueFlow)
  }

  private def executeStatement(
      statement: Statement,
      environment: Environment,
      context: ExecutionContext,
      insideRoutine: Boolean
  ): Either[RuntimeError, ControlFlow] = {
    statement match {
      case VariableDeclaration(name, initializer, position) =>
        val valueEither = initializer match {
          case Some(expression) => evaluate(expression, environment, context)
          case None => Right(NullValue: Value)
        }
        valueEither.map { value =>
          environment.define(name, value)
          ContinueFlow
        }

      case Assignment(name, expression, position) =>
        evaluate(expression, environment, context).flatMap { value =>
          environment.assign(name, value, position).map(_ => ContinueFlow)
        }

      case PrintStatement(expression, _) =>
        evaluate(expression, environment, context).map { value =>
          context.outputs += value.render
          ContinueFlow
        }

      case ReturnStatement(expression, position) =>
        if (!insideRoutine) {
          Left(RuntimeError("'entregar_resultado' solo puede usarse dentro de una rutina.", position))
        } else {
          val returnValueEither = expression match {
            case Some(value) => evaluate(value, environment, context)
            case None => Right(NullValue: Value)
          }
          returnValueEither.map(value => new ReturnFlow(value))
        }

      case AdjustWeightStatement(name, amount, isIncrease, position) =>
        adjustWeight(name, amount, isIncrease, environment, context, position)

      case IfStatement(condition, thenBranch, elseBranch, _) =>
        evaluate(condition, environment, context).flatMap { value =>
          asBoolean(value, condition.position).flatMap { boolean =>
            if (boolean) executeBlock(thenBranch, environment, context, insideRoutine)
            else elseBranch.map(executeBlock(_, environment, context, insideRoutine)).getOrElse(Right(ContinueFlow))
          }
        }

      case WhileStatement(condition, body, position) =>
        executeWhile(condition, body, environment, context, insideRoutine, position)

      case RoutineDeclaration(name, parameters, body, _) =>
        val routine = RoutineValue(name, parameters, body, environment)
        environment.define(name, routine)
        Right(ContinueFlow)

      case CallStatement(name, arguments, position) =>
        invokeCall(CallExpression(name, arguments, position), environment, context).map(_ => ContinueFlow)

      case ChangeSetStatement(name, index, value, position) =>
        mutateListVariable(name, environment, context, position) { list =>
          for {
            evaluatedIndex <- evaluate(index, environment, context)
            evaluatedValue <- evaluate(value, environment, context)
            indexValue <- asWholeNumber(evaluatedIndex, position)
            updated <- changeSet(list, indexValue, evaluatedValue, position, name)
          } yield updated
        }

      case AddSetStatement(name, value, position) =>
        mutateListVariable(name, environment, context, position) { list =>
          evaluate(value, environment, context).flatMap { evaluatedValue =>
            appendToList(list, evaluatedValue, position, name)
          }
        }

      case RemoveSetStatement(name, index, position) =>
        mutateListVariable(name, environment, context, position) { list =>
          evaluate(index, environment, context).flatMap { evaluatedIndex =>
            asWholeNumber(evaluatedIndex, position).flatMap(removeFromList(list, _, position, name))
          }
        }

      case block: Block =>
        executeBlock(block, environment, context, insideRoutine)

      case ExpressionStatement(expression, _) =>
        evaluate(expression, environment, context).map(_ => ContinueFlow)
    }
  }

  private def executeBlock(
      block: Block,
      environment: Environment,
      context: ExecutionContext,
      insideRoutine: Boolean
  ): Either[RuntimeError, ControlFlow] = {
    executeStatements(block.statements, environment.child, context, insideRoutine)
  }

  private def executeWhile(
      condition: Expression,
      body: Block,
      environment: Environment,
      context: ExecutionContext,
      insideRoutine: Boolean,
      position: Position
  ): Either[RuntimeError, ControlFlow] = {
    var iterations = 0

    while (true) {
      if (iterations >= maxLoopIterations) {
        return Left(RuntimeError(s"Se alcanzo el limite de $maxLoopIterations iteraciones para evitar una rutina infinita.", position))
      }

      val conditionValue = evaluate(condition, environment, context)
      val conditionBoolean = conditionValue.flatMap(asBoolean(_, condition.position))

      conditionBoolean match {
        case Left(error) => return Left(error)
        case Right(false) => return Right(ContinueFlow)
        case Right(true) =>
          executeBlock(body, environment, context, insideRoutine) match {
            case Left(error) => return Left(error)
            case Right(flow: ReturnFlow) => return Right(flow)
            case Right(ContinueFlow) =>
              iterations += 1
          }
      }
    }

    Right(ContinueFlow)
  }

  private def evaluate(
      expression: Expression,
      environment: Environment,
      context: ExecutionContext
  ): Either[RuntimeError, Value] = {
    expression match {
      case LiteralExpression(value, _) =>
        Right(toRuntimeValue(value))

      case VariableExpression(name, position) =>
        environment.resolve(name, position)

      case GroupingExpression(inner, _) =>
        evaluate(inner, environment, context)

      case CallExpression(name, arguments, position) =>
        invokeCall(CallExpression(name, arguments, position), environment, context)

      case ListExpression(elements, position) =>
        evaluateAll(elements, environment, context).flatMap(values => buildHomogeneousList(values, position, None))

      case TakeExpression(collection, indexExpression, position) =>
        for {
          collectionValue <- evaluate(collection, environment, context)
          indexValue <- evaluate(indexExpression, environment, context)
          index <- asWholeNumber(indexValue, position)
          result <- takeFromList(collectionValue, index, position)
        } yield result

      case LengthExpression(collection, position) =>
        evaluate(collection, environment, context).flatMap(lengthOfList(_, position))

      case RangeExpression(collection, startExpression, endExpression, position) =>
        for {
          collectionValue <- evaluate(collection, environment, context)
          startValue <- evaluate(startExpression, environment, context)
          endValue <- evaluate(endExpression, environment, context)
          start <- asWholeNumber(startValue, position)
          end <- asWholeNumber(endValue, position)
          result <- sliceList(collectionValue, start, end, position)
        } yield result

      case UnaryExpression(operator, inner, position) =>
        evaluate(inner, environment, context).flatMap(value => applyUnary(operator, value, position))

      case BinaryExpression(left, operator, right, position) =>
        for {
          leftValue <- evaluate(left, environment, context)
          rightValue <- evaluate(right, environment, context)
          result <- applyBinary(leftValue, operator, rightValue, position)
        } yield result
    }
  }

  private def evaluateAll(
      expressions: List[Expression],
      environment: Environment,
      context: ExecutionContext
  ): Either[RuntimeError, List[Value]] = {
    expressions.foldLeft[Either[RuntimeError, List[Value]]](Right(Nil)) {
      case (accEither, expression) =>
        for {
          acc <- accEither
          value <- evaluate(expression, environment, context)
        } yield acc :+ value
    }
  }

  private def invokeCall(
      call: CallExpression,
      environment: Environment,
      context: ExecutionContext
  ): Either[RuntimeError, Value] = {
    for {
      callee <- environment.resolve(call.name, call.position)
      arguments <- evaluateAll(call.arguments, environment, context)
      value <- callee match {
        case routine: RoutineValue => callRoutine(routine, arguments, context, call.position)
        case _ => Left(RuntimeError(s"'${call.name}' no es una rutina invocable.", call.position))
      }
    } yield value
  }

  private def callRoutine(
      routine: RoutineValue,
      arguments: List[Value],
      context: ExecutionContext,
      position: Position
  ): Either[RuntimeError, Value] = {
    if (routine.parameters.length != arguments.length) {
      Left(
        RuntimeError(
          s"La rutina '${routine.name}' esperaba ${routine.parameters.length} argumento(s) y recibio ${arguments.length}.",
          position
        )
      )
    } else {
      context.callDepth += 1
      if (context.callDepth > maxCallDepth) {
        context.callDepth -= 1
        Left(RuntimeError(s"La rutina '${routine.name}' supero la profundidad maxima de llamadas ($maxCallDepth).", position))
      } else {
        val localEnvironment = routine.parameters.zip(arguments).foldLeft(routine.closure.child) {
          case (env, (parameter, argument)) => env.define(parameter, argument)
        }

        val result = executeStatements(routine.body.statements, localEnvironment, context, insideRoutine = true).map {
          case flow: ReturnFlow => flow.value
          case ContinueFlow => NullValue
        }

        context.callDepth -= 1
        result
      }
    }
  }

  private def adjustWeight(
      name: String,
      amount: Option[Expression],
      isIncrease: Boolean,
      environment: Environment,
      context: ExecutionContext,
      position: Position
  ): Either[RuntimeError, ControlFlow] = {
    val deltaEither = amount match {
      case Some(expression) => evaluate(expression, environment, context)
      case None => Right(NumberValue(1): Value)
    }

    for {
      currentValue <- environment.resolve(name, position)
      deltaValue <- deltaEither
      currentNumber <- asNumber(currentValue, position, s"La variable '$name' debe ser numerica para ${if (isIncrease) "subir_peso" else "bajar_peso"}.")
      deltaNumber <- asNumber(deltaValue, position, s"La cantidad usada en '${if (isIncrease) "subir_peso" else "bajar_peso"}' debe ser numerica.")
      nextValue = if (isIncrease) NumberValue(currentNumber + deltaNumber) else NumberValue(currentNumber - deltaNumber)
      _ <- environment.assign(name, nextValue, position)
    } yield ContinueFlow
  }

  private def mutateListVariable(
      name: String,
      environment: Environment,
      context: ExecutionContext,
      position: Position
  )(
      transform: ListValue => Either[RuntimeError, ListValue]
  ): Either[RuntimeError, ControlFlow] = {
    environment.resolve(name, position).flatMap {
      case list: ListValue =>
        transform(list).flatMap { updated =>
          environment.assign(name, updated, position).map(_ => ContinueFlow)
        }
      case _ =>
        Left(RuntimeError(s"'$name' no es una lista valida para esta operacion.", position))
    }
  }

  private def applyUnary(operator: TokenType, value: Value, position: Position): Either[RuntimeError, Value] = {
    operator match {
      case TokenType.Minus =>
        asNumber(value, position, "El operador 'menos_reps' requiere un numero.").map(number => NumberValue(-number))

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
    for {
      leftNumber <- asNumber(left, position, s"La operacion '$operatorName' requiere dos numeros.")
      rightNumber <- asNumber(right, position, s"La operacion '$operatorName' requiere dos numeros.")
    } yield NumberValue(operation(leftNumber, rightNumber))
  }

  private def numericComparison(
      left: Value,
      right: Value,
      position: Position,
      operatorName: String,
      operation: (BigDecimal, BigDecimal) => Boolean
  ): Either[RuntimeError, Value] = {
    for {
      leftNumber <- asNumber(left, position, s"La comparacion '$operatorName' requiere dos numeros.")
      rightNumber <- asNumber(right, position, s"La comparacion '$operatorName' requiere dos numeros.")
    } yield BooleanValue(operation(leftNumber, rightNumber))
  }

  private def asBoolean(value: Value, position: Position): Either[RuntimeError, Boolean] = {
    value match {
      case BooleanValue(boolean) => Right(boolean)
      case _ => Left(RuntimeError("La condicion del entrenamiento debe evaluarse a verdadero o falso.", position))
    }
  }

  private def asNumber(value: Value, position: Position, message: String): Either[RuntimeError, BigDecimal] = {
    value match {
      case NumberValue(number) => Right(number)
      case _ => Left(RuntimeError(message, position))
    }
  }

  private def asWholeNumber(value: Value, position: Position): Either[RuntimeError, Int] = {
    value match {
      case NumberValue(number) if number.isValidInt && number == BigDecimal(number.toInt) && number >= 0 =>
        Right(number.toInt)
      case _ =>
        Left(RuntimeError("El indice debe ser un numero entero no negativo.", position))
    }
  }

  private def buildHomogeneousList(
      values: List[Value],
      position: Position,
      listName: Option[String]
  ): Either[RuntimeError, ListValue] = {
    if (values.isEmpty) {
      Right(ListValue(Vector.empty, None))
    } else {
      val firstType = runtimeTypeName(values.head)
      val invalid = values.find(value => runtimeTypeName(value) != firstType)
      invalid match {
        case Some(other) =>
          val prefix = listName.map(name => s"La lista '$name'").getOrElse("La lista")
          Left(RuntimeError(s"$prefix mezcla tipos incompatibles: $firstType y ${runtimeTypeName(other)}.", position))
        case None =>
          Right(ListValue(values.toVector, Some(firstType)))
      }
    }
  }

  private def appendToList(
      list: ListValue,
      value: Value,
      position: Position,
      listName: String
  ): Either[RuntimeError, ListValue] = {
    ensureListElementCompatibility(list, value, position, listName).map { elementType =>
      ListValue(list.values :+ value, elementType)
    }
  }

  private def changeSet(
      list: ListValue,
      index: Int,
      value: Value,
      position: Position,
      listName: String
  ): Either[RuntimeError, ListValue] = {
    if (!list.values.isDefinedAt(index)) {
      Left(RuntimeError(s"El indice $index esta fuera de rango para la lista '$listName'.", position))
    } else {
      ensureListElementCompatibility(list, value, position, listName).map { elementType =>
        ListValue(list.values.updated(index, value), elementType)
      }
    }
  }

  private def removeFromList(
      list: ListValue,
      index: Int,
      position: Position,
      listName: String
  ): Either[RuntimeError, ListValue] = {
    if (!list.values.isDefinedAt(index)) {
      Left(RuntimeError(s"El indice $index esta fuera de rango para la lista '$listName'.", position))
    } else {
      val updated = list.values.patch(index, Nil, 1)
      val elementType = if (updated.isEmpty) None else Some(runtimeTypeName(updated.head))
      Right(ListValue(updated, elementType))
    }
  }

  private def ensureListElementCompatibility(
      list: ListValue,
      value: Value,
      position: Position,
      listName: String
  ): Either[RuntimeError, Option[String]] = {
    list.elementTypeName match {
      case Some(expectedType) if expectedType != runtimeTypeName(value) =>
        Left(RuntimeError(s"La lista '$listName' mezcla tipos incompatibles: $expectedType y ${runtimeTypeName(value)}.", position))
      case Some(expectedType) =>
        Right(Some(expectedType))
      case None =>
        Right(Some(runtimeTypeName(value)))
    }
  }

  private def takeFromList(value: Value, index: Int, position: Position): Either[RuntimeError, Value] = {
    value match {
      case ListValue(values, _) =>
        values.lift(index).toRight(RuntimeError(s"El indice $index esta fuera de rango.", position))
      case _ =>
        Left(RuntimeError("La operacion 'tomar' solo funciona sobre listas.", position))
    }
  }

  private def lengthOfList(value: Value, position: Position): Either[RuntimeError, Value] = {
    value match {
      case ListValue(values, _) => Right(NumberValue(values.length))
      case _ => Left(RuntimeError("La operacion 'largo' solo funciona sobre listas.", position))
    }
  }

  private def sliceList(value: Value, start: Int, end: Int, position: Position): Either[RuntimeError, Value] = {
    value match {
      case ListValue(values, elementType) if start <= end && start <= values.length && end <= values.length =>
        Right(ListValue(values.slice(start, end), elementType))
      case ListValue(_, _) =>
        Left(RuntimeError("El rango solicitado no es valido para la lista.", position))
      case _ =>
        Left(RuntimeError("La operacion 'rango_set' solo funciona sobre listas.", position))
    }
  }

  private def runtimeTypeName(value: Value): String = {
    value match {
      case NumberValue(_) => "numero"
      case StringValue(_) => "texto"
      case BooleanValue(_) => "booleano"
      case ListValue(_, _) => "lista"
      case RoutineValue(_, _, _, _) => "rutina"
      case NullValue => "nulo"
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
