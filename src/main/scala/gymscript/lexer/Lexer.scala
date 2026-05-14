package gymscript.lexer

import gymscript.util.Position

import scala.collection.mutable.ListBuffer

final class Lexer {
  def tokenize(source: String): Either[List[LexicalError], List[Token]] = {
    val tokens = ListBuffer.empty[Token]
    val errors = ListBuffer.empty[LexicalError]

    var index = 0
    var position = Position.Start

    while (index < source.length) {
      val start = position
      source.charAt(index) match {
        case ' ' | '\t' =>
          index += 1
          position = position.advance()

        case '\r' =>
          index += 1
          position = position.advance()

        case '\n' =>
          tokens += Token(TokenType.NewLine, "\n", start)
          index += 1
          position = position.nextLine

        case '#' =>
          val outcome = readComment(source, index, position)
          index = outcome.nextIndex
          position = outcome.nextPosition

        case '(' =>
          tokens += Token(TokenType.LeftParen, "(", start)
          index += 1
          position = position.advance()

        case ')' =>
          tokens += Token(TokenType.RightParen, ")", start)
          index += 1
          position = position.advance()

        case '{' =>
          tokens += Token(TokenType.LeftBrace, "{", start)
          index += 1
          position = position.advance()

        case '}' =>
          tokens += Token(TokenType.RightBrace, "}", start)
          index += 1
          position = position.advance()

        case ',' =>
          tokens += Token(TokenType.Comma, ",", start)
          index += 1
          position = position.advance()

        case '+' =>
          tokens += Token(TokenType.Plus, "+", start)
          index += 1
          position = position.advance()

        case '-' =>
          tokens += Token(TokenType.Minus, "-", start)
          index += 1
          position = position.advance()

        case '*' =>
          tokens += Token(TokenType.Star, "*", start)
          index += 1
          position = position.advance()

        case '/' =>
          tokens += Token(TokenType.Slash, "/", start)
          index += 1
          position = position.advance()

        case '=' if peek(source, index + 1).contains('=') =>
          tokens += Token(TokenType.EqualEqual, "==", start)
          index += 2
          position = position.advance(2)

        case '=' =>
          tokens += Token(TokenType.Assign, "=", start)
          index += 1
          position = position.advance()

        case '!' if peek(source, index + 1).contains('=') =>
          tokens += Token(TokenType.BangEqual, "!=", start)
          index += 2
          position = position.advance(2)

        case '!' =>
          errors += LexicalError(
            "El simbolo '!' aislado no es valido. Usa 'no' o '!='.",
            start,
            Some("!")
          )
          index += 1
          position = position.advance()

        case '"' =>
          val outcome = readString(source, index, position)
          tokens ++= outcome.tokens
          errors ++= outcome.errors
          index = outcome.nextIndex
          position = outcome.nextPosition

        case current if current.isDigit =>
          val outcome = readNumber(source, index, position)
          tokens ++= outcome.tokens
          errors ++= outcome.errors
          index = outcome.nextIndex
          position = outcome.nextPosition

        case current if isIdentifierStart(current) =>
          val outcome = readIdentifier(source, index, position)
          tokens += outcome.token
          index = outcome.nextIndex
          position = outcome.nextPosition

        case other =>
          errors += LexicalError(s"Caracter no reconocido: '$other'.", start, Some(other.toString))
          index += 1
          position = position.advance()
      }
    }

    tokens += Token(TokenType.EOF, "", position)

    if (errors.nonEmpty) Left(errors.toList) else Right(tokens.toList)
  }

  private final class ScanOutcome(
      val nextIndex: Int,
      val nextPosition: Position,
      val tokens: List[Token] = Nil,
      val errors: List[LexicalError] = Nil
  )

  private final class IdentifierOutcome(val token: Token, val nextIndex: Int, val nextPosition: Position)

  private def readComment(source: String, startIndex: Int, startPosition: Position): ScanOutcome = {
    var index = startIndex
    var position = startPosition

    while (index < source.length && source.charAt(index) != '\n') {
      index += 1
      position = position.advance()
    }

    new ScanOutcome(index, position)
  }

  private def readString(source: String, startIndex: Int, startPosition: Position): ScanOutcome = {
    var index = startIndex + 1
    var position = startPosition.advance()
    val builder = new StringBuilder
    var closed = false

    while (index < source.length && !closed) {
      source.charAt(index) match {
        case '"' =>
          closed = true
          index += 1
          position = position.advance()

        case '\n' =>
          val lexeme = "\"" + builder.toString()
          return new ScanOutcome(index, position, errors = List(LexicalError("String sin cierre.", startPosition, Some(lexeme))))

        case '\\' =>
          if (index + 1 >= source.length) {
            val lexeme = "\"" + builder.toString() + "\\"
            return new ScanOutcome(
              index + 1,
              position.advance(),
              errors = List(LexicalError("Secuencia de escape incompleta en string.", startPosition, Some(lexeme)))
            )
          }

          val escaped = source.charAt(index + 1)
          val translated = escaped match {
            case '"' => Some('"')
            case 'n' => Some('\n')
            case 't' => Some('\t')
            case '\\' => Some('\\')
            case _ => None
          }

          translated match {
            case Some(value) =>
              builder.append(value)
              index += 2
              position = position.advance(2)

            case None =>
              val lexeme = s"\\$escaped"
              return new ScanOutcome(
                index + 2,
                position.advance(2),
                errors = List(
                  LexicalError(
                    s"Secuencia de escape no soportada: '\\$escaped'.",
                    position,
                    Some(lexeme)
                  )
                )
              )
          }

        case current =>
          builder.append(current)
          index += 1
          position = position.advance()
      }
    }

    if (!closed) {
      val lexeme = "\"" + builder.toString()
      new ScanOutcome(index, position, errors = List(LexicalError("String sin cierre.", startPosition, Some(lexeme))))
    } else {
      val literal = builder.toString()
      new ScanOutcome(
        index,
        position,
        tokens = List(Token(TokenType.StringLiteral, "\"" + literal + "\"", startPosition, Some(literal)))
      )
    }
  }

  private def readNumber(source: String, startIndex: Int, startPosition: Position): ScanOutcome = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder

    while (peek(source, index).exists(_.isDigit)) {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    if (peek(source, index).contains('.')) {
      if (peek(source, index + 1).exists(_.isDigit)) {
        builder.append('.')
        index += 1
        position = position.advance()

        while (peek(source, index).exists(_.isDigit)) {
          builder.append(source.charAt(index))
          index += 1
          position = position.advance()
        }
      } else {
        return invalidNumericLexeme(source, index, position, startPosition, builder.toString() + ".", "Numero decimal mal formado.")
      }
    }

    if (peek(source, index).contains('.')) {
      return invalidNumericLexeme(source, index, position, startPosition, builder.toString(), "Numero decimal mal formado.")
    }

    if (peek(source, index).exists(isIdentifierStart)) {
      return invalidIdentifierAfterNumber(source, index, position, startPosition, builder.toString())
    }

    val lexeme = builder.toString()
    new ScanOutcome(index, position, tokens = List(Token(TokenType.Number, lexeme, startPosition, Some(lexeme))))
  }

  private def readIdentifier(source: String, startIndex: Int, startPosition: Position): IdentifierOutcome = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder

    while (peek(source, index).exists(isIdentifierPart)) {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    val lexeme = builder.toString()
    val tokenType = TokenType.keywords.getOrElse(lexeme, TokenType.Identifier)
    new IdentifierOutcome(Token(tokenType, lexeme, startPosition, Some(lexeme)), index, position)
  }

  private def invalidNumericLexeme(
      source: String,
      index: Int,
      position: Position,
      startPosition: Position,
      prefix: String,
      message: String
  ): ScanOutcome = {
    val (lexeme, nextIndex, nextPosition) = readMalformedSequence(
      source,
      index,
      position,
      prefix,
      ch => ch.isLetterOrDigit || ch == '_' || ch == '.'
    )
    new ScanOutcome(nextIndex, nextPosition, errors = List(LexicalError(message, startPosition, Some(lexeme))))
  }

  private def invalidIdentifierAfterNumber(
      source: String,
      index: Int,
      position: Position,
      startPosition: Position,
      numericPrefix: String
  ): ScanOutcome = {
    val (lexeme, nextIndex, nextPosition) = readMalformedSequence(
      source,
      index,
      position,
      numericPrefix,
      isIdentifierPart
    )
    new ScanOutcome(
      nextIndex,
      nextPosition,
      errors = List(LexicalError("Identificador invalido: no puede iniciar con un numero.", startPosition, Some(lexeme)))
    )
  }

  private def readMalformedSequence(
      source: String,
      startIndex: Int,
      startPosition: Position,
      prefix: String,
      predicate: Char => Boolean
  ): (String, Int, Position) = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder(prefix)

    while (peek(source, index).exists(predicate)) {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    (builder.toString(), index, position)
  }

  private def isIdentifierStart(ch: Char): Boolean = ch.isLetter || ch == '_'

  private def isIdentifierPart(ch: Char): Boolean = ch.isLetterOrDigit || ch == '_'

  private def peek(source: String, index: Int): Option[Char] = {
    if (index >= 0 && index < source.length) Some(source.charAt(index)) else None
  }
}
