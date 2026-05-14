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

object SemanticAnalyzer {
  sealed trait StaticType {
    def displayName: String
  }

  object StaticType {
    case object NumberType extends StaticType { val displayName = "numero" }
    case object StringType extends StaticType { val displayName = "texto" }
    case object BooleanType extends StaticType { val displayName = "booleano" }
    case object NullType extends StaticType { val displayName = "nulo" }
    case object RoutineType extends StaticType { val displayName = "rutina" }
    case object UnknownType extends StaticType { val displayName = "desconocido" }
    final case class ListType(elementType: Option[StaticType]) extends StaticType {
      val displayName = elementType.map(t => s"lista de ${t.displayName}").getOrElse("lista vacia")
    }
  }

  sealed trait SymbolKind
  case object VariableSymbol extends SymbolKind
  final case class RoutineSymbol(arity: Int) extends SymbolKind
  final case class SymbolInfo(staticType: StaticType, kind: SymbolKind)
}

final class SemanticAnalyzer {
  import SemanticAnalyzer._
  import SemanticAnalyzer.StaticType._

  def analyze(program: Program): Either[List[SemanticError], Program] = {
    new Analyzer(program).run()
  }

  private final class Analyzer(program: Program) {
    private val errors = mutable.ListBuffer.empty[SemanticError]
    private val scopes = mutable.ArrayBuffer(mutable.LinkedHashMap.empty[String, SymbolInfo])
    private var routineDepth = 0

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
            error(position, s"La variable '$name' ya fue declarada en este alcance.", Some(name))
          } else {
            val inferredType = initializer.map(analyzeExpression).getOrElse(UnknownType)
            currentScope.update(name, SymbolInfo(inferredType, VariableSymbol))
          }

        case Assignment(name, expression, position) =>
          val expressionType = analyzeExpression(expression)
          resolveSymbol(name) match {
            case Some((scope, info)) if info.kind == VariableSymbol =>
              scope.update(name, info.copy(staticType = mergeTypes(info.staticType, expressionType)))
            case Some((_, _)) =>
              error(position, s"'$name' es una rutina y no puede reasignarse como variable.", Some(name))
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
          }

        case PrintStatement(expression, _) =>
          analyzeExpression(expression)

        case ReturnStatement(expression, position) =>
          if (routineDepth <= 0) {
            error(position, "'entregar_resultado' solo puede usarse dentro de una rutina.", None)
          }
          expression.foreach(analyzeExpression)

        case AdjustWeightStatement(name, amount, isIncrease, position) =>
          resolveSymbol(name) match {
            case Some((scope, info)) if info.kind == VariableSymbol =>
              requireType(
                info.staticType,
                NumberType,
                position,
                s"La variable '$name' debe ser numerica para ${if (isIncrease) "subir_peso" else "bajar_peso"}."
              )
              amount.foreach { expression =>
                val amountType = analyzeExpression(expression)
                requireType(
                  amountType,
                  NumberType,
                  position,
                  s"La cantidad usada en '${if (isIncrease) "subir_peso" else "bajar_peso"}' debe ser numerica."
                )
              }
              scope.update(name, info.copy(staticType = NumberType))
            case Some((_, _)) =>
              error(position, s"'$name' es una rutina y no puede ajustarse con peso.", Some(name))
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
          }

        case IfStatement(condition, thenBranch, elseBranch, _) =>
          validateBooleanCondition(condition, "si_fuerza")
          withScope(analyzeStatements(thenBranch.statements))
          elseBranch.foreach(block => withScope(analyzeStatements(block.statements)))

        case WhileStatement(condition, body, _) =>
          validateBooleanCondition(condition, "mientras_entrenas")
          withScope(analyzeStatements(body.statements))

        case RoutineDeclaration(name, parameters, body, position) =>
          if (currentScope.contains(name)) {
            error(position, s"La rutina '$name' ya fue declarada en este alcance.", Some(name))
          } else {
            currentScope.update(name, SymbolInfo(RoutineType, RoutineSymbol(parameters.length)))
          }
          withScope {
            routineDepth += 1
            try {
              parameters.foreach { parameter =>
                if (currentScope.contains(parameter)) {
                  error(position, s"El parametro '$parameter' esta repetido en la rutina '$name'.", Some(parameter))
                } else {
                  currentScope.update(parameter, SymbolInfo(UnknownType, VariableSymbol))
                }
              }
              analyzeStatements(body.statements)
            } finally {
              routineDepth -= 1
            }
          }

        case CallStatement(name, arguments, position) =>
          validateCall(name, arguments, position)

        case ChangeSetStatement(name, index, value, position) =>
          validateListMutation(name, index, Some(value), position, expectsValue = true)

        case AddSetStatement(name, value, position) =>
          validateListMutation(name, LiteralExpression(NumberLiteral(0), position), Some(value), position, expectsValue = true)

        case RemoveSetStatement(name, index, position) =>
          validateListMutation(name, index, None, position, expectsValue = false)

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
          resolveSymbol(name) match {
            case Some((_, info)) => info.staticType
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
              UnknownType
          }

        case GroupingExpression(inner, _) =>
          analyzeExpression(inner)

        case CallExpression(name, arguments, position) =>
          validateCall(name, arguments, position)
          UnknownType

        case ListExpression(elements, position) =>
          inferListType(elements.map(analyzeExpression), position, None)

        case TakeExpression(collection, index, position) =>
          val collectionType = analyzeExpression(collection)
          val indexType = analyzeExpression(index)
          validateListType(collectionType, position, "La operacion 'tomar' requiere una lista.")
          requireType(indexType, NumberType, position, "La posicion de 'tomar' debe ser numerica.")
          collectionType match {
            case ListType(Some(elementType)) => elementType
            case _ => UnknownType
          }

        case LengthExpression(collection, position) =>
          val collectionType = analyzeExpression(collection)
          validateListType(collectionType, position, "La operacion 'largo' requiere una lista.")
          NumberType

        case RangeExpression(collection, start, end, position) =>
          val collectionType = analyzeExpression(collection)
          val startType = analyzeExpression(start)
          val endType = analyzeExpression(end)
          validateListType(collectionType, position, "La operacion 'rango_set' requiere una lista.")
          requireType(startType, NumberType, position, "El inicio de 'rango_set' debe ser numerico.")
          requireType(endType, NumberType, position, "El fin de 'rango_set' debe ser numerico.")
          collectionType match {
            case listType: ListType => listType
            case _ => ListType(None)
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
          val leftType = analyzeExpression(left)
          val rightType = analyzeExpression(right)
          analyzeBinaryExpression(leftType, operator, rightType, position)
      }
    }

    private def validateCall(name: String, arguments: List[Expression], position: Position): Unit = {
      arguments.foreach(analyzeExpression)
      resolveSymbol(name) match {
        case Some((_, SymbolInfo(_, RoutineSymbol(arity)))) if arity == arguments.length =>
          ()
        case Some((_, SymbolInfo(_, RoutineSymbol(arity)))) =>
          error(position, s"La rutina '$name' esperaba $arity argumento(s) y recibio ${arguments.length}.", Some(name))
        case Some((_, SymbolInfo(_, VariableSymbol))) =>
          error(position, s"'$name' es una variable y no puede invocarse como rutina.", Some(name))
        case None =>
          error(position, s"La rutina '$name' se uso antes de ser declarada.", Some(name))
      }
    }

    private def validateListMutation(
        name: String,
        index: Expression,
        value: Option[Expression],
        position: Position,
        expectsValue: Boolean
    ): Unit = {
      val indexType = analyzeExpression(index)
      requireType(indexType, NumberType, position, "El indice de la operacion de lista debe ser numerico.")

      resolveSymbol(name) match {
        case Some((scope, info)) if info.kind == VariableSymbol =>
          validateListType(info.staticType, position, s"La variable '$name' debe ser una lista.")
          val updatedListType = info.staticType match {
            case listType: ListType =>
              value match {
                case Some(expression) =>
                  val valueType = analyzeExpression(expression)
                  ensureListElementType(listType, valueType, position, name)
                case None => listType
              }
            case other => other
          }
          scope.update(name, info.copy(staticType = updatedListType))
        case Some((_, _)) =>
          error(position, s"'$name' es una rutina y no puede operarse como lista.", Some(name))
        case None =>
          error(position, s"La variable '$name' se uso antes de ser declarada.", Some(name))
      }

      if (!expectsValue && value.nonEmpty) {
        error(position, "La operacion no esperaba un valor adicional.", None)
      }
    }

    private def analyzeBinaryExpression(
        leftType: StaticType,
        operator: TokenType,
        rightType: StaticType,
        position: Position
    ): StaticType = {
      operator match {
        case TokenType.Plus =>
          (leftType, rightType) match {
            case (NumberType, NumberType) => NumberType
            case (StringType, _) => StringType
            case (_, StringType) => StringType
            case (UnknownType, _) | (_, UnknownType) => UnknownType
            case _ =>
              error(position, "La operacion 'mas_reps' solo admite numeros o concatenacion con strings.", None)
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

    private def inferListType(elementTypes: List[StaticType], position: Position, listName: Option[String]): StaticType = {
      if (elementTypes.isEmpty) {
        ListType(None)
      } else {
        val filtered = elementTypes.filterNot(_ == UnknownType)
        filtered match {
          case Nil => ListType(None)
          case head :: tail if tail.forall(isSameType(_, head)) =>
            ListType(Some(head))
          case head :: tail =>
            val other = tail.find(t => !isSameType(t, head)).getOrElse(head)
            val prefix = listName.map(name => s"La lista '$name'").getOrElse("La lista")
            error(position, s"$prefix mezcla tipos incompatibles: ${head.displayName} y ${other.displayName}.", None)
            ListType(None)
        }
      }
    }

    private def ensureListElementType(
        listType: ListType,
        valueType: StaticType,
        position: Position,
        name: String
    ): ListType = {
      listType.elementType match {
        case Some(expected) if !isCompatible(valueType, expected) =>
          error(position, s"La lista '$name' mezcla tipos incompatibles: ${expected.displayName} y ${valueType.displayName}.", Some(name))
          listType
        case Some(_) =>
          listType
        case None =>
          ListType(Some(valueType))
      }
    }

    private def validateBooleanCondition(expression: Expression, context: String): Unit = {
      val expressionType = analyzeExpression(expression)
      if (expressionType != BooleanType && expressionType != UnknownType) {
        error(expression.position, s"La condicion de '$context' debe ser booleana.", None)
      }
    }

    private def validateNumericOperands(
        leftType: StaticType,
        rightType: StaticType,
        position: Position,
        operator: String
    ): Unit = {
      if (!isCompatible(leftType, NumberType) || !isCompatible(rightType, NumberType)) {
        error(position, s"El operador '$operator' requiere operandos numericos.", None)
      }
    }

    private def validateBooleanOperands(
        leftType: StaticType,
        rightType: StaticType,
        position: Position,
        operator: String
    ): Unit = {
      if (!isCompatible(leftType, BooleanType) || !isCompatible(rightType, BooleanType)) {
        error(position, s"El operador '$operator' requiere operandos booleanos.", None)
      }
    }

    private def validateListType(actual: StaticType, position: Position, message: String): Unit = {
      actual match {
        case _: ListType | UnknownType => ()
        case _ => error(position, message, None)
      }
    }

    private def requireType(actual: StaticType, expected: StaticType, position: Position, message: String): Unit = {
      if (!isCompatible(actual, expected)) {
        error(position, message, None)
      }
    }

    private def mergeTypes(previous: StaticType, next: StaticType): StaticType = {
      (previous, next) match {
        case (UnknownType, value) => value
        case (value, UnknownType) => value
        case (currentList: ListType, nextList: ListType) =>
          ListType(currentList.elementType.orElse(nextList.elementType))
        case (_, value) => value
      }
    }

    private def isCompatible(actual: StaticType, expected: StaticType): Boolean = {
      actual == expected ||
      actual == UnknownType ||
      ((actual, expected) match {
        case (ListType(None), _: ListType) => true
        case (ListType(Some(a)), ListType(Some(b))) => isSameType(a, b)
        case (ListType(_), ListType(None)) => true
        case _ => false
      })
    }

    private def isSameType(left: StaticType, right: StaticType): Boolean = {
      (left, right) match {
        case (ListType(a), ListType(b)) => a == b
        case _ => left == right
      }
    }

    private def withScope(block: => Unit): Unit = {
      scopes.append(mutable.LinkedHashMap.empty[String, SymbolInfo])
      try block
      finally scopes.remove(scopes.length - 1)
    }

    private def currentScope: mutable.LinkedHashMap[String, SymbolInfo] = scopes.last

    private def resolveSymbol(name: String): Option[(mutable.LinkedHashMap[String, SymbolInfo], SymbolInfo)] = {
      scopes.reverseIterator
        .flatMap(scope => scope.get(name).map(info => scope -> info))
        .toSeq
        .headOption
    }

    private def error(position: Position, message: String, context: Option[String]): Unit = {
      errors += SemanticError(message, position, context)
    }
  }
}
