package gymscript.resolver

import gymscript.lexer.TokenType
import gymscript.parser._
import gymscript.util.Position

import scala.collection.mutable

final case class SemanticError(message: String, position: Position) {
  def render: String = s"Error semantico en ${position.render}: $message"
}

object SemanticAnalyzer {
  sealed trait StaticType {
    def displayName: String
  }

  object StaticType {
    case object NumberType extends StaticType { val displayName = "numero" }
    case object StringType extends StaticType { val displayName = "string" }
    case object BooleanType extends StaticType { val displayName = "booleano" }
    case object NullType extends StaticType { val displayName = "null" }
    case object ListType extends StaticType { val displayName = "lista" }
    case object RoutineType extends StaticType { val displayName = "rutina" }
    case object UnknownType extends StaticType { val displayName = "desconocido" }
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
            error(position, s"La variable '$name' se uso mas de una vez en el mismo alcance.")
          } else {
            val inferredType = initializer.map(analyzeExpression).getOrElse(UnknownType)
            currentScope.update(name, SymbolInfo(inferredType, VariableSymbol))
          }

        case Assignment(name, expression, position) =>
          val expressionType = analyzeExpression(expression)
          resolveSymbol(name) match {
            case Some((scope, info)) if info.kind == VariableSymbol =>
              scope.update(name, info.copy(staticType = expressionType))
            case Some((_, _)) =>
              error(position, s"'$name' es una rutina y no puede reasignarse como variable.")
            case None =>
              error(position, s"La variable '$name' se uso antes de ser declarada.")
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

        case RoutineDeclaration(name, parameters, body, position) =>
          if (currentScope.contains(name)) {
            error(position, s"La rutina '$name' ya fue declarada en este alcance.")
          } else {
            currentScope.update(name, SymbolInfo(RoutineType, RoutineSymbol(parameters.length)))
          }
          withScope {
            parameters.foreach { parameter =>
              if (currentScope.contains(parameter)) {
                error(position, s"El parametro '$parameter' esta repetido en la rutina '$name'.")
              } else {
                currentScope.update(parameter, SymbolInfo(UnknownType, VariableSymbol))
              }
            }
            analyzeStatements(body.statements)
          }

        case CallStatement(name, arguments, position) =>
          arguments.foreach(analyzeExpression)
          resolveSymbol(name) match {
            case Some((_, SymbolInfo(_, RoutineSymbol(arity)))) if arity == arguments.length =>
              ()
            case Some((_, SymbolInfo(_, RoutineSymbol(arity)))) =>
              error(position, s"La rutina '$name' esperaba $arity argumento(s) y recibio ${arguments.length}.")
            case Some((_, SymbolInfo(_, VariableSymbol))) =>
              error(position, s"'$name' es una variable y no puede invocarse como rutina.")
            case None =>
              error(position, s"La rutina '$name' se uso antes de ser declarada.")
          }

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
              error(position, s"La variable '$name' se uso antes de ser declarada.")
              UnknownType
          }

        case GroupingExpression(inner, _) =>
          analyzeExpression(inner)

        case ListExpression(elements, _) =>
          elements.foreach(analyzeExpression)
          ListType

        case TakeExpression(collection, index, position) =>
          val collectionType = analyzeExpression(collection)
          val indexType = analyzeExpression(index)
          requireType(collectionType, ListType, position, "La operacion 'tomar' requiere una lista.")
          requireType(indexType, NumberType, position, "La posicion de 'tomar' debe ser numerica.")
          UnknownType

        case LengthExpression(collection, position) =>
          val collectionType = analyzeExpression(collection)
          requireType(collectionType, ListType, position, "La operacion 'largo' requiere una lista.")
          NumberType

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
              error(position, "La operacion 'mas_reps' solo admite numeros o concatenacion con strings.")
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
      if (expressionType != BooleanType && expressionType != UnknownType) {
        error(expression.position, s"La condicion de '$context' debe ser booleana.")
      }
    }

    private def validateNumericOperands(
        leftType: StaticType,
        rightType: StaticType,
        position: Position,
        operator: String
    ): Unit = {
      if (!isCompatible(leftType, NumberType) || !isCompatible(rightType, NumberType)) {
        error(position, s"El operador '$operator' requiere operandos numericos.")
      }
    }

    private def validateBooleanOperands(
        leftType: StaticType,
        rightType: StaticType,
        position: Position,
        operator: String
    ): Unit = {
      if (!isCompatible(leftType, BooleanType) || !isCompatible(rightType, BooleanType)) {
        error(position, s"El operador '$operator' requiere operandos booleanos.")
      }
    }

    private def requireType(actual: StaticType, expected: StaticType, position: Position, message: String): Unit = {
      if (!isCompatible(actual, expected)) {
        error(position, message)
      }
    }

    private def isCompatible(actual: StaticType, expected: StaticType): Boolean = {
      actual == expected || actual == UnknownType
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

    private def error(position: Position, message: String): Unit = {
      errors += SemanticError(message, position)
    }
  }
}
