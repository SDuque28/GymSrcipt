package gymscript.resolver

import gymscript.lexer.TokenType
import gymscript.parser._
import gymscript.util.Position

import scala.collection.mutable

final case class SemanticError(message: String, position: Position, context: Option[String] = None) {
  def render: String = {
    val contextDetail = context.map(value => s" Context: '$value'.").getOrElse("")
    s"[SEMANTIC ERROR] line ${position.line}, column ${position.column}: $message$contextDetail"
  }
}

final class SemanticAnalyzer {
  import GymType._

  def analyze(program: Program): Either[List[SemanticError], Program] = {
    new Analyzer(program).run()
  }

  private final class Analyzer(program: Program) {
    private final class RoutineSignature(
        val declaration: RoutineDeclaration,
        val parameterTypes: List[GymType],
        val declaredReturnType: Option[GymType],
        var inferredReturnType: Option[GymType] = None,
        var analyzing: Boolean = false,
        var analyzed: Boolean = false
    ) {
      def name: String = declaration.name

      def effectiveReturnType: GymType =
        inferredReturnType
          .orElse(declaredReturnType)
          .getOrElse(UnknownType)
    }

    private final class RoutineContext(val name: String, val expectedReturnType: Option[GymType])
    private final class FlowInfo(val returnTypes: List[GymType], val mayFallThrough: Boolean)

    private val errors = mutable.ListBuffer.empty[SemanticError]
    private val scopes = mutable.ArrayBuffer(mutable.LinkedHashMap.empty[String, GymType])
    private val routineSignatures = mutable.LinkedHashMap.empty[String, RoutineSignature]
    private var currentRoutine: Option[RoutineContext] = None

    def run(): Either[List[SemanticError], Program] = {
      collectTopLevelRoutineSignatures(program.statements)
      routineSignatures.keys.foreach(ensureRoutineAnalyzed)
      analyzeTopLevelStatements(program.statements)
      if (errors.nonEmpty) Left(errors.toList) else Right(program)
    }

    private def collectTopLevelRoutineSignatures(statements: List[Statement]): Unit = {
      statements.foreach {
        case declaration: RoutineDeclaration =>
          if (routineSignatures.contains(declaration.name) || currentScope.contains(declaration.name)) {
            error(declaration.position, s"La rutina '${declaration.name}' ya fue declarada en este alcance.", Some(declaration.name))
          } else {
            val parameterTypes = declaration.parameters.map { parameter =>
              parameter.typeAnnotation.map(resolveTypeAnnotation).getOrElse(UnknownType)
            }
            val declaredReturnType = declaration.returnType.map(resolveTypeAnnotation)
            routineSignatures.update(declaration.name, new RoutineSignature(declaration, parameterTypes, declaredReturnType))
          }
        case _ => ()
      }
    }

    private def analyzeTopLevelStatements(statements: List[Statement]): Unit = {
      statements.foreach {
        case declaration: RoutineDeclaration =>
          ensureRoutineAnalyzed(declaration.name)
        case statement =>
          analyzeStatement(statement, topLevel = true)
      }
    }

    private def ensureRoutineAnalyzed(name: String): GymType = {
      routineSignatures.get(name) match {
        case Some(signature) if signature.analyzed =>
          signature.effectiveReturnType

        case Some(signature) if signature.analyzing =>
          signature.declaredReturnType.getOrElse(signature.effectiveReturnType)

        case Some(signature) =>
          signature.analyzing = true
          val flow = withScope {
            withRoutineContext(new RoutineContext(signature.name, signature.declaredReturnType)) {
              registerParameters(signature.declaration, signature.parameterTypes)
              analyzeStatements(signature.declaration.body.statements, topLevel = false)
            }
          }

          val inferredReturn = inferRoutineReturnType(signature, flow)
          signature.inferredReturnType = Some(inferredReturn)
          signature.analyzing = false
          signature.analyzed = true
          inferredReturn

        case None =>
          UnknownType
      }
    }

    private def registerParameters(declaration: RoutineDeclaration, parameterTypes: List[GymType]): Unit = {
      declaration.parameters.zip(parameterTypes).foreach {
        case (parameter, parameterType) =>
          if (currentScope.contains(parameter.name)) {
            error(
              parameter.position,
              s"El parametro '${parameter.name}' esta repetido en la rutina '${declaration.name}'.",
              Some(parameter.name)
            )
          } else {
            currentScope.update(parameter.name, parameterType)
          }
      }
    }

    private def inferRoutineReturnType(signature: RoutineSignature, flow: FlowInfo): GymType = {
      val actualReturnType =
        if (flow.returnTypes.isEmpty) {
          NullType
        } else {
          unifyReturnTypes(flow.returnTypes, signature.declaration.position, signature.name)
        }

      signature.declaredReturnType match {
        case Some(expected) =>
          if (expected != NullType && flow.returnTypes.isEmpty) {
            error(
              signature.declaration.position,
              s"La rutina '${signature.name}' declara retorno ${expected.displayName}, pero no entrega ningun resultado.",
              Some(signature.name)
            )
          }

          if (expected != NullType && flow.mayFallThrough) {
            error(
              signature.declaration.position,
              s"La rutina '${signature.name}' puede finalizar sin 'entregar_resultado', pero declara retorno ${expected.displayName}.",
              Some(signature.name)
            )
          }
          expected

        case None if flow.returnTypes.isEmpty =>
          NullType

        case None if flow.mayFallThrough =>
          error(
            signature.declaration.position,
            s"La rutina '${signature.name}' mezcla caminos con y sin 'entregar_resultado'. Declara 'entrega ...' o retorna en todos los caminos.",
            Some(signature.name)
          )
          actualReturnType

        case None =>
          actualReturnType
      }
    }

    private def analyzeStatements(statements: List[Statement], topLevel: Boolean): FlowInfo = {
      val returnTypes = mutable.ListBuffer.empty[GymType]
      var mayFallThrough = true

      statements.foreach { statement =>
        val statementFlow = analyzeStatement(statement, topLevel)
        returnTypes ++= statementFlow.returnTypes
        if (mayFallThrough) {
          mayFallThrough = statementFlow.mayFallThrough
        }
      }

      new FlowInfo(returnTypes.toList, mayFallThrough)
    }

    private def analyzeStatement(statement: Statement, topLevel: Boolean): FlowInfo = {
      statement match {
        case VariableDeclaration(name, initializer, position) =>
          if (currentScope.contains(name) || routineSignatures.contains(name)) {
            error(position, s"La variable '$name' ya fue declarada en este alcance.", Some(name))
          } else {
            val inferredType = initializer.map(analyzeExpression).getOrElse(UnknownType)
            currentScope.update(name, inferredType)
          }
          new FlowInfo(Nil, mayFallThrough = true)

        case Assignment(name, expression, position) =>
          val expressionType = analyzeExpression(expression)
          resolveVariable(name) match {
            case Some((scope, currentType)) =>
              if (!isCompatible(expressionType, currentType)) {
                error(
                  position,
                  s"La variable '$name' tiene tipo ${currentType.displayName}, pero recibio ${expressionType.displayName}.",
                  Some(name)
                )
              }
              scope.update(name, mergeVariableTypes(currentType, expressionType))
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
          }
          new FlowInfo(Nil, mayFallThrough = true)

        case PrintStatement(expression, _) =>
          analyzeExpression(expression)
          new FlowInfo(Nil, mayFallThrough = true)

        case ReturnStatement(expression, position) =>
          if (currentRoutine.isEmpty) {
            error(position, "'entregar_resultado' solo puede usarse dentro de una rutina.", None)
          }
          val returnType = expression.map(analyzeExpression).getOrElse(NullType)
          currentRoutine.flatMap(_.expectedReturnType).foreach { expected =>
            if (!isCompatible(returnType, expected)) {
              error(
                position,
                s"La rutina '${currentRoutine.get.name}' declara retorno ${expected.displayName}, pero entrega ${returnType.displayName}.",
                Some(currentRoutine.get.name)
              )
            }
          }
          new FlowInfo(List(returnType), mayFallThrough = false)

        case AdjustWeightStatement(name, amount, isIncrease, position) =>
          resolveVariable(name) match {
            case Some((scope, variableType)) =>
              requireType(
                variableType,
                NumberType,
                position,
                s"La variable '$name' debe ser numerica para ${if (isIncrease) "subir_peso" else "bajar_peso"}."
              )
              amount.foreach { expression =>
                requireType(
                  analyzeExpression(expression),
                  NumberType,
                  position,
                  s"La cantidad usada en '${if (isIncrease) "subir_peso" else "bajar_peso"}' debe ser numerica."
                )
              }
              scope.update(name, NumberType)
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
          }
          new FlowInfo(Nil, mayFallThrough = true)

        case IfStatement(condition, thenBranch, elseBranch, _) =>
          validateBooleanCondition(condition, "si_fuerza")
          val thenFlow = withScope(analyzeStatements(thenBranch.statements, topLevel = false))
          val elseFlow = elseBranch.map(block => withScope(analyzeStatements(block.statements, topLevel = false)))
          new FlowInfo(
            thenFlow.returnTypes ++ elseFlow.toList.flatMap(_.returnTypes),
            mayFallThrough = elseFlow.forall(_.mayFallThrough) || thenFlow.mayFallThrough
          )

        case WhileStatement(condition, body, _) =>
          validateBooleanCondition(condition, "mientras_entrenas")
          val bodyFlow = withScope(analyzeStatements(body.statements, topLevel = false))
          new FlowInfo(bodyFlow.returnTypes, mayFallThrough = true)

        case declaration: RoutineDeclaration =>
          if (!topLevel) {
            error(declaration.position, "GymScript solo permite declarar rutinas en el nivel superior del archivo.", Some(declaration.name))
          } else {
            ensureRoutineAnalyzed(declaration.name)
          }
          new FlowInfo(Nil, mayFallThrough = true)

        case CallStatement(name, arguments, position) =>
          analyzeCall(name, arguments, position)
          new FlowInfo(Nil, mayFallThrough = true)

        case ChangeSetStatement(name, index, value, position) =>
          validateListMutation(name, index, Some(value), position)
          new FlowInfo(Nil, mayFallThrough = true)

        case AddSetStatement(name, value, position) =>
          validateListMutation(name, LiteralExpression(NumberLiteral(0), position), Some(value), position)
          new FlowInfo(Nil, mayFallThrough = true)

        case RemoveSetStatement(name, index, position) =>
          validateListMutation(name, index, None, position)
          new FlowInfo(Nil, mayFallThrough = true)

        case Block(statements, _) =>
          withScope(analyzeStatements(statements, topLevel = false))

        case ExpressionStatement(expression, _) =>
          analyzeExpression(expression)
          new FlowInfo(Nil, mayFallThrough = true)
      }
    }

    private def analyzeExpression(expression: Expression): GymType = {
      expression match {
        case LiteralExpression(value, _) =>
          value match {
            case NumberLiteral(_) => NumberType
            case StringLiteral(_) => StringType
            case BooleanLiteral(_) => BooleanType
            case NullLiteral => NullType
          }

        case VariableExpression(name, position) =>
          resolveVariable(name) match {
            case Some((_, variableType)) => variableType
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
              UnknownType
          }

        case GroupingExpression(inner, _) =>
          analyzeExpression(inner)

        case CallExpression(name, arguments, position) =>
          analyzeCall(name, arguments, position)

        case ListExpression(elements, position) =>
          inferListType(elements.map(analyzeExpression), position, None)

        case TakeExpression(collection, index, position) =>
          val collectionType = analyzeExpression(collection)
          requireType(analyzeExpression(index), NumberType, position, "La posicion de 'tomar' debe ser numerica.")
          collectionType match {
            case ListType(elementType) => elementType
            case UnknownType => UnknownType
            case _ =>
              error(position, "La operacion 'tomar' requiere una lista.", None)
              UnknownType
          }

        case LengthExpression(collection, position) =>
          validateListExpression(analyzeExpression(collection), position, "La operacion 'largo' requiere una lista.")
          NumberType

        case RangeExpression(collection, start, end, position) =>
          val collectionType = analyzeExpression(collection)
          requireType(analyzeExpression(start), NumberType, position, "El inicio de 'rango_set' debe ser numerico.")
          requireType(analyzeExpression(end), NumberType, position, "El fin de 'rango_set' debe ser numerico.")
          collectionType match {
            case listType: ListType => listType
            case UnknownType => ListType(UnknownType)
            case _ =>
              error(position, "La operacion 'rango_set' requiere una lista.", None)
              ListType(UnknownType)
          }

        case UnaryExpression(operator, inner, position) =>
          val innerType = analyzeExpression(inner)
          operator match {
            case TokenType.Minus =>
              requireType(innerType, NumberType, position, "El operador 'menos_reps' requiere un numero.")
              NumberType
            case TokenType.Not =>
              requireType(innerType, BooleanType, position, "El operador 'sin_energia' requiere un booleano.")
              BooleanType
            case _ =>
              UnknownType
          }

        case BinaryExpression(left, operator, right, position) =>
          analyzeBinaryExpression(analyzeExpression(left), operator, analyzeExpression(right), position)
      }
    }

    private def analyzeCall(name: String, arguments: List[Expression], position: Position): GymType = {
      val argumentTypes = arguments.map(analyzeExpression)
      routineSignatures.get(name) match {
        case Some(signature) =>
          if (signature.parameterTypes.length != argumentTypes.length) {
            error(
              position,
              s"La rutina '$name' esperaba ${signature.parameterTypes.length} argumento(s) y recibio ${argumentTypes.length}.",
              Some(name)
            )
          }

          signature.declaration.parameters.zip(signature.parameterTypes).zip(argumentTypes).foreach {
            case ((parameter, expectedType), actualType) =>
              if (!isCompatible(actualType, expectedType)) {
                error(
                  position,
                  s"El parametro '${parameter.name}' de la rutina '$name' espera ${expectedType.displayName}, pero recibio ${actualType.displayName}.",
                  Some(parameter.name)
                )
              }
          }

          ensureRoutineAnalyzed(name)

        case None =>
          error(position, s"La rutina '$name' se uso antes de ser declarada.", Some(name))
      }

      routineSignatures.get(name).map(_.effectiveReturnType).getOrElse(UnknownType)
    }

    private def validateListMutation(
        name: String,
        index: Expression,
        value: Option[Expression],
        position: Position
    ): Unit = {
      requireType(analyzeExpression(index), NumberType, position, "El indice de la operacion de lista debe ser numerico.")

      resolveVariable(name) match {
        case Some((scope, listType @ ListType(elementType))) =>
          value match {
            case Some(expression) =>
              val valueType = analyzeExpression(expression)
              val updatedType = ensureListElementType(listType, valueType, position, name)
              scope.update(name, updatedType)
            case None =>
              scope.update(name, ListType(elementType))
          }
        case Some((_, UnknownType)) =>
          value.foreach(analyzeExpression)
        case Some((_, otherType)) =>
          error(position, s"La variable '$name' debe ser una lista, pero es ${otherType.displayName}.", Some(name))
        case None =>
          error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
      }
    }

    private def analyzeBinaryExpression(
        leftType: GymType,
        operator: TokenType,
        rightType: GymType,
        position: Position
    ): GymType = {
      operator match {
        case TokenType.Plus =>
          (leftType, rightType) match {
            case (NumberType, NumberType) => NumberType
            case (StringType, _) => StringType
            case (_, StringType) => StringType
            case (UnknownType, _) | (_, UnknownType) => UnknownType
            case _ =>
              error(position, "La operacion 'mas_reps' solo admite numeros o concatenacion con texto.", None)
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
          if (!areComparable(leftType, rightType)) {
            error(
              position,
              s"La comparacion '${operator.lexemeName}' requiere operandos compatibles y recibio ${leftType.displayName} y ${rightType.displayName}.",
              None
            )
          }
          BooleanType

        case _ =>
          UnknownType
      }
    }

    private def validateBooleanCondition(expression: Expression, context: String): Unit = {
      requireType(analyzeExpression(expression), BooleanType, expression.position, s"La condicion de '$context' debe ser booleana.")
    }

    private def validateNumericOperands(leftType: GymType, rightType: GymType, position: Position, operator: String): Unit = {
      if (!isCompatible(leftType, NumberType) || !isCompatible(rightType, NumberType)) {
        error(position, s"El operador '$operator' requiere operandos numericos.", None)
      }
    }

    private def validateBooleanOperands(leftType: GymType, rightType: GymType, position: Position, operator: String): Unit = {
      if (!isCompatible(leftType, BooleanType) || !isCompatible(rightType, BooleanType)) {
        error(position, s"El operador '$operator' requiere operandos booleanos.", None)
      }
    }

    private def validateListExpression(actualType: GymType, position: Position, message: String): Unit = {
      actualType match {
        case _: ListType | UnknownType => ()
        case _ => error(position, message, None)
      }
    }

    private def inferListType(elementTypes: List[GymType], position: Position, listName: Option[String]): GymType = {
      if (elementTypes.isEmpty) {
        ListType(UnknownType)
      } else {
        val knownTypes = elementTypes.filterNot(_ == UnknownType)
        if (knownTypes.isEmpty) {
          ListType(UnknownType)
        } else {
          val head = knownTypes.head
          val incompatible = knownTypes.find(other => !isCompatible(other, head) || !isCompatible(head, other))
          incompatible match {
            case Some(other) =>
              val prefix = listName.map(name => s"La lista '$name'").getOrElse("La lista")
              error(position, s"$prefix mezcla tipos incompatibles: ${head.displayName} y ${other.displayName}.", None)
              ListType(UnknownType)
            case None =>
              ListType(head)
          }
        }
      }
    }

    private def ensureListElementType(
        listType: ListType,
        valueType: GymType,
        position: Position,
        name: String
    ): ListType = {
      listType.elementType match {
        case UnknownType =>
          ListType(valueType)
        case elementType if !isCompatible(valueType, elementType) =>
          error(position, s"La lista '$name' mezcla tipos incompatibles: ${elementType.displayName} y ${valueType.displayName}.", Some(name))
          listType
        case elementType =>
          ListType(elementType)
      }
    }

    private def resolveTypeAnnotation(annotation: TypeAnnotation): GymType = {
      annotation match {
        case SimpleTypeAnnotation("numero", _) => NumberType
        case SimpleTypeAnnotation("texto", _) => StringType
        case SimpleTypeAnnotation("booleano", _) => BooleanType
        case SimpleTypeAnnotation("sin_resultado", _) => NullType
        case ListTypeAnnotation(elementType, position) =>
          val resolvedElementType = resolveTypeAnnotation(elementType)
          if (resolvedElementType == NullType) {
            error(position, "Una 'lista_de' no puede contener elementos 'sin_resultado'.", None)
            ListType(UnknownType)
          } else {
            ListType(resolvedElementType)
          }
        case SimpleTypeAnnotation(other, position) =>
          error(position, s"Tipo no reconocido en GymScript: '$other'.", Some(other))
          UnknownType
      }
    }

    private def unifyReturnTypes(types: List[GymType], position: Position, routineName: String): GymType = {
      types.foldLeft[GymType](UnknownType) { (acc, next) =>
        unifyTypes(acc, next).getOrElse {
          error(
            position,
            s"La rutina '$routineName' tiene retornos incompatibles: ${acc.displayName} y ${next.displayName}.",
            Some(routineName)
          )
          UnknownType
        }
      }
    }

    private def unifyTypes(left: GymType, right: GymType): Option[GymType] = {
      (left, right) match {
        case (UnknownType, other) => Some(other)
        case (other, UnknownType) => Some(other)
        case (a, b) if a == b => Some(a)
        case (ListType(a), ListType(b)) =>
          unifyTypes(a, b).map(ListType.apply)
        case _ => None
      }
    }

    private def requireType(actual: GymType, expected: GymType, position: Position, message: String): Unit = {
      if (!isCompatible(actual, expected)) {
        error(position, message, None)
      }
    }

    private def mergeVariableTypes(previous: GymType, next: GymType): GymType = {
      unifyTypes(previous, next).getOrElse(previous)
    }

    private def isCompatible(actual: GymType, expected: GymType): Boolean = {
      actual == UnknownType ||
      expected == UnknownType ||
      actual == expected ||
      ((actual, expected) match {
        case (ListType(UnknownType), ListType(_)) => true
        case (ListType(_), ListType(UnknownType)) => true
        case (ListType(actualElementType), ListType(expectedElementType)) =>
          isCompatible(actualElementType, expectedElementType)
        case _ => false
      })
    }

    private def areComparable(left: GymType, right: GymType): Boolean = {
      isCompatible(left, right) || isCompatible(right, left)
    }

    private def withScope[A](block: => A): A = {
      scopes.append(mutable.LinkedHashMap.empty[String, GymType])
      try block
      finally scopes.remove(scopes.length - 1)
    }

    private def withRoutineContext[A](context: RoutineContext)(block: => A): A = {
      val previous = currentRoutine
      currentRoutine = Some(context)
      try block
      finally currentRoutine = previous
    }

    private def resolveVariable(name: String): Option[(mutable.LinkedHashMap[String, GymType], GymType)] = {
      scopes.reverseIterator.flatMap(scope => scope.get(name).map(scope -> _)).toSeq.headOption
    }

    private def currentScope: mutable.LinkedHashMap[String, GymType] = scopes.last

    private def error(position: Position, message: String, context: Option[String]): Unit = {
      errors += SemanticError(message, position, context)
    }
  }
}
