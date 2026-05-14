package gymscript.parser

import gymscript.lexer.{ Token, TokenType }
import gymscript.util.Position

import scala.collection.mutable.ListBuffer

final class Parser {
  def parse(tokens: List[Token]): Either[List[ParseError], Program] = {
    val state = new ParserState(tokens.toVector)
    state.parseProgram()
  }

  private final class ParserState(tokens: Vector[Token]) {
    private val errors = ListBuffer.empty[ParseError]
    private var current = 0

    def parseProgram(): Either[List[ParseError], Program] = {
      val statements = ListBuffer.empty[Statement]
      skipNewLines()

      while (!isAtEnd) {
        parseStatement(topLevel = true) match {
          case Some(statement) => statements += statement
          case None => synchronize()
        }
        skipNewLines()
      }

      val position = tokens.headOption.map(_.position).getOrElse(Position.Start)
      if (errors.nonEmpty) Left(errors.toList) else Right(Program(statements.toList, position))
    }

    private def parseStatement(topLevel: Boolean): Option[Statement] = {
      try {
        peek.tokenType match {
          case TokenType.Peso => Some(parseVariableDeclaration())
          case TokenType.Mostrar => Some(parsePrintStatement())
          case TokenType.SiFuerza => Some(parseIfStatement())
          case TokenType.MientrasEntrenas => Some(parseWhileStatement())
          case TokenType.Rutina => Some(parseRoutineDeclaration())
          case TokenType.Llamar => Some(parseCallStatement())
          case TokenType.LeftBrace => Some(parseLegacyBraceBlock())
          case TokenType.Descanso if topLevel =>
            reportAndAdvance("Se encontro 'descanso' sin un bloque 'si_fuerza' activo.")
            None
          case TokenType.FinRutina if topLevel =>
            reportAndAdvance("Se encontro 'fin_rutina' sin un bloque de entrenamiento abierto.")
            None
          case TokenType.Identifier if checkNext(TokenType.Assign) =>
            Some(parseAssignment())
          case TokenType.EOF =>
            None
          case _ =>
            Some(parseExpressionStatement())
        }
      } catch {
        case ParserFailure => None
      }
    }

    private def parseVariableDeclaration(): Statement = {
      val keyword = advance()
      val nameToken = consume(TokenType.Identifier, "Se esperaba el nombre de la variable despues de 'peso'.")
      val initializer =
        if (matchType(TokenType.Assign)) Some(parseExpression())
        else {
          error(peek.position, "Se esperaba 'cargar' despues del nombre de la variable.")
          None
        }
      VariableDeclaration(nameToken.lexeme, initializer, keyword.position)
    }

    private def parseAssignment(): Statement = {
      val nameToken = consume(TokenType.Identifier, "Se esperaba el nombre de la variable.")
      consume(TokenType.Assign, "Se esperaba 'cargar' en la asignacion.")
      val expression = parseExpression()
      Assignment(nameToken.lexeme, expression, nameToken.position)
    }

    private def parsePrintStatement(): Statement = {
      val keyword = advance()
      consume(TokenType.LeftParen, "Se esperaba 'abre_set' despues de 'mostrar'.")
      val expression = parseExpression()
      consume(TokenType.RightParen, "Se esperaba 'cierra_set' para cerrar 'mostrar'.")
      PrintStatement(expression, keyword.position)
    }

    private def parseIfStatement(): Statement = {
      val keyword = advance()
      val condition = parseExpression()
      consume(TokenType.InicioRutina, "Se esperaba 'inicio_rutina' despues de la condicion de 'si_fuerza'.")
      val thenStatements = parseRequiredBlockStatements(Set(TokenType.Descanso, TokenType.FinRutina), "si_fuerza")
      val thenBranch = Block(thenStatements, keyword.position)

      val elseBranch =
        if (matchType(TokenType.Descanso)) {
          val elsePosition = previous.position
          consume(TokenType.InicioRutina, "Se esperaba 'inicio_rutina' despues de 'descanso'.")
          Some(Block(parseRequiredBlockStatements(Set(TokenType.FinRutina), "descanso"), elsePosition))
        } else {
          None
        }

      consume(TokenType.FinRutina, "Se esperaba 'fin_rutina' para cerrar el bloque de 'si_fuerza'.")
      IfStatement(condition, thenBranch, elseBranch, keyword.position)
    }

    private def parseWhileStatement(): Statement = {
      val keyword = advance()
      val condition = parseExpression()
      consume(TokenType.InicioRutina, "Se esperaba 'inicio_rutina' despues de la condicion de 'mientras_entrenas'.")
      val bodyStatements = parseRequiredBlockStatements(Set(TokenType.FinRutina), "mientras_entrenas")
      consume(TokenType.FinRutina, "Se esperaba 'fin_rutina' para cerrar el bloque de 'mientras_entrenas'.")
      WhileStatement(condition, Block(bodyStatements, keyword.position), keyword.position)
    }

    private def parseRoutineDeclaration(): Statement = {
      val keyword = advance()
      val nameToken = consume(TokenType.Identifier, "Se esperaba el nombre de la rutina.")
      val parameters = parseIdentifierList("Se esperaba 'abre_set' despues del nombre de la rutina.")
      consume(TokenType.InicioRutina, "Se esperaba 'inicio_rutina' despues de la firma de la rutina.")
      val bodyStatements = parseRequiredBlockStatements(Set(TokenType.FinRutina), s"rutina '${nameToken.lexeme}'")
      consume(TokenType.FinRutina, "Se esperaba 'fin_rutina' para cerrar la rutina.")
      RoutineDeclaration(nameToken.lexeme, parameters, Block(bodyStatements, keyword.position), keyword.position)
    }

    private def parseCallStatement(): Statement = {
      val keyword = advance()
      val nameToken = consume(TokenType.Identifier, "Se esperaba el nombre de la rutina a invocar.")
      val arguments = parseArgumentList("Se esperaba 'abre_set' despues del nombre de la rutina.")
      CallStatement(nameToken.lexeme, arguments, keyword.position)
    }

    private def parseLegacyBraceBlock(): Statement = {
      val opening = consume(TokenType.LeftBrace, "Se esperaba '{'.")
      val statements = ListBuffer.empty[Statement]
      skipNewLines()

      while (!check(TokenType.RightBrace) && !isAtEnd) {
        parseStatement(topLevel = false) match {
          case Some(statement) => statements += statement
          case None => synchronize(stopTokens = Set(TokenType.RightBrace))
        }
        skipNewLines()
      }

      consume(TokenType.RightBrace, "Se esperaba '}' para cerrar el bloque legacy.")
      Block(statements.toList, opening.position)
    }

    private def parseRequiredBlockStatements(terminators: Set[TokenType], owner: String): List[Statement] = {
      val statements = parseStatementsUntil(terminators)
      if (statements.isEmpty) {
        error(previous.position, s"El bloque '$owner' no puede estar vacio.")
      }
      statements
    }

    private def parseStatementsUntil(terminators: Set[TokenType]): List[Statement] = {
      val statements = ListBuffer.empty[Statement]
      skipNewLines()

      while (!checkAny(terminators) && !isAtEnd) {
        parseStatement(topLevel = false) match {
          case Some(statement) => statements += statement
          case None => synchronize(stopTokens = terminators)
        }
        skipNewLines()
      }

      statements.toList
    }

    private def parseExpressionStatement(): Statement = {
      val expression = parseExpression()
      ExpressionStatement(expression, expression.position)
    }

    private def parseExpression(): Expression = parseOr()

    private def parseOr(): Expression = parseLeftAssociative(parseAnd _, Set(TokenType.Or))

    private def parseAnd(): Expression = parseLeftAssociative(parseEquality _, Set(TokenType.And))

    private def parseEquality(): Expression = parseLeftAssociative(parseComparison _, Set(TokenType.EqualEqual, TokenType.BangEqual))

    private def parseComparison(): Expression = {
      parseLeftAssociative(
        parseTerm _,
        Set(TokenType.GreaterThan, TokenType.LessThan, TokenType.GreaterEqual, TokenType.LessEqual)
      )
    }

    private def parseTerm(): Expression = parseLeftAssociative(parseFactor _, Set(TokenType.Plus, TokenType.Minus))

    private def parseFactor(): Expression = parseLeftAssociative(parseUnary _, Set(TokenType.Star, TokenType.Slash))

    private def parseLeftAssociative(next: () => Expression, operators: Set[TokenType]): Expression = {
      var expression = next()

      while (checkAny(operators)) {
        val operator = advance()
        val right = next()
        expression = BinaryExpression(expression, operator.tokenType, right, expression.position)
      }

      expression
    }

    private def parseUnary(): Expression = {
      if (matchType(TokenType.Not, TokenType.Minus)) {
        val operator = previous
        val right = parseUnary()
        UnaryExpression(operator.tokenType, right, operator.position)
      } else {
        parsePrimary()
      }
    }

    private def parsePrimary(): Expression = {
      peek.tokenType match {
        case TokenType.Number =>
          val token = advance()
          LiteralExpression(NumberLiteral(BigDecimal(token.literal.getOrElse(token.lexeme))), token.position)

        case TokenType.StringLiteral =>
          val token = advance()
          LiteralExpression(StringLiteral(token.literal.getOrElse("")), token.position)

        case TokenType.Verdadero =>
          val token = advance()
          LiteralExpression(BooleanLiteral(value = true), token.position)

        case TokenType.Falso =>
          val token = advance()
          LiteralExpression(BooleanLiteral(value = false), token.position)

        case TokenType.Identifier =>
          val token = advance()
          VariableExpression(token.lexeme, token.position)

        case TokenType.LeftParen =>
          val opening = advance()
          val expression = parseExpression()
          consume(TokenType.RightParen, "Se esperaba 'cierra_set' para cerrar la expresion.")
          GroupingExpression(expression, opening.position)

        case TokenType.Lista =>
          parseListExpression()

        case TokenType.Tomar =>
          parseTakeExpression()

        case TokenType.Largo =>
          parseLengthExpression()

        case TokenType.NewLine | TokenType.EOF | TokenType.FinRutina | TokenType.Descanso | TokenType.RightBrace =>
          fail("Se esperaba una expresion valida de entrenamiento.")

        case TokenType.InicioRutina =>
          fail("Se encontro 'inicio_rutina' donde GymScript esperaba una expresion.")

        case _ =>
          fail(s"Token inesperado en expresion: '${peek.lexeme}'.")
      }
    }

    private def parseListExpression(): Expression = {
      val keyword = advance()
      val elements = parseArgumentList("Se esperaba 'abre_set' despues de 'lista'.")
      ListExpression(elements, keyword.position)
    }

    private def parseTakeExpression(): Expression = {
      val keyword = advance()
      consume(TokenType.LeftParen, "Se esperaba 'abre_set' despues de 'tomar'.")
      val collection = parseExpression()
      consume(TokenType.Comma, "Se esperaba 'separa' para indicar la posicion en 'tomar'.")
      val index = parseExpression()
      consume(TokenType.RightParen, "Se esperaba 'cierra_set' para cerrar 'tomar'.")
      TakeExpression(collection, index, keyword.position)
    }

    private def parseLengthExpression(): Expression = {
      val keyword = advance()
      consume(TokenType.LeftParen, "Se esperaba 'abre_set' despues de 'largo'.")
      val collection = parseExpression()
      consume(TokenType.RightParen, "Se esperaba 'cierra_set' para cerrar 'largo'.")
      LengthExpression(collection, keyword.position)
    }

    private def parseIdentifierList(openingMessage: String): List[String] = {
      consume(TokenType.LeftParen, openingMessage)
      val parameters = ListBuffer.empty[String]

      if (!check(TokenType.RightParen)) {
        do {
          val parameter = consume(TokenType.Identifier, "Se esperaba el nombre de un parametro.")
          parameters += parameter.lexeme
        } while (matchType(TokenType.Comma))
      }

      consume(TokenType.RightParen, "Se esperaba 'cierra_set' para cerrar la lista de parametros.")
      parameters.toList
    }

    private def parseArgumentList(openingMessage: String): List[Expression] = {
      consume(TokenType.LeftParen, openingMessage)
      val arguments = ListBuffer.empty[Expression]

      if (!check(TokenType.RightParen)) {
        do {
          arguments += parseExpression()
        } while (matchType(TokenType.Comma))
      }

      consume(TokenType.RightParen, "Se esperaba 'cierra_set' para cerrar la lista de argumentos.")
      arguments.toList
    }

    private def skipNewLines(): Unit = {
      while (matchType(TokenType.NewLine)) {}
    }

    private def synchronize(stopTokens: Set[TokenType] = Set.empty): Unit = {
      while (
        !isAtEnd &&
        !check(TokenType.NewLine) &&
        !statementStartTokens.contains(peek.tokenType) &&
        !stopTokens.contains(peek.tokenType)
      ) {
        advance()
      }

      if (check(TokenType.NewLine)) {
        skipNewLines()
      }
    }

    private val statementStartTokens: Set[TokenType] = Set(
      TokenType.Peso,
      TokenType.Mostrar,
      TokenType.SiFuerza,
      TokenType.MientrasEntrenas,
      TokenType.Rutina,
      TokenType.Llamar,
      TokenType.LeftBrace,
      TokenType.Identifier,
      TokenType.Descanso,
      TokenType.FinRutina
    )

    private def matchType(expected: TokenType*): Boolean = {
      expected.exists { tokenType =>
        if (check(tokenType)) {
          advance()
          true
        } else {
          false
        }
      }
    }

    private def consume(expected: TokenType, message: String): Token = {
      if (check(expected)) advance()
      else {
        error(peek.position, message)
        syntheticToken(expected)
      }
    }

    private def syntheticToken(tokenType: TokenType): Token = Token(tokenType, "", peek.position)

    private def reportAndAdvance(message: String): Unit = {
      error(peek.position, message)
      advance()
    }

    private def fail(message: String): Nothing = {
      error(peek.position, message)
      throw ParserFailure
    }

    private def error(position: Position, message: String): Unit = {
      errors += ParseError(message, position)
    }

    private def check(expected: TokenType): Boolean = peek.tokenType == expected

    private def checkNext(expected: TokenType): Boolean = tokens.lift(current + 1).exists(_.tokenType == expected)

    private def checkAny(expected: Set[TokenType]): Boolean = expected.contains(peek.tokenType)

    private def advance(): Token = {
      val token = peek
      if (!isAtEnd) {
        current += 1
      }
      token
    }

    private def isAtEnd: Boolean = peek.tokenType == TokenType.EOF

    private def peek: Token = tokens.lift(current).getOrElse(tokens.lastOption.getOrElse(Token(TokenType.EOF, "", Position.Start)))

    private def previous: Token = tokens.lift(math.max(current - 1, 0)).getOrElse(Token(TokenType.EOF, "", Position.Start))

    private object ParserFailure extends RuntimeException
  }
}
