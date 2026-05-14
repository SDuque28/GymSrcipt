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
      val current = source.charAt(index)
      val start = position

      current match {
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
          val (comment, nextIndex, nextPosition) = readComment(source, index, position)
          tokens += Token(TokenType.Comment, comment, start, Some(comment.drop(1).trim))
          index = nextIndex
          position = nextPosition

        case '/' if peek(source, index + 1).contains('/') =>
          val (comment, nextIndex, nextPosition) = readSlashComment(source, index, position)
          tokens += Token(TokenType.Comment, comment, start, Some(comment.drop(2).trim))
          index = nextIndex
          position = nextPosition

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
          errors += LexicalError("El simbolo '!' aislado no es valido. Usa 'no' o '!='.", start)
          index += 1
          position = position.advance()

        case '"' =>
          readString(source, index, position) match {
            case Left(error) =>
              errors += error
              index += 1
              position = position.advance()

            case Right((token, nextIndex, nextPosition)) =>
              tokens += token
              index = nextIndex
              position = nextPosition
          }

        case ch if ch.isDigit =>
          val (token, nextIndex, nextPosition) = readNumber(source, index, position)
          tokens += token
          index = nextIndex
          position = nextPosition

        case ch if isIdentifierStart(ch) =>
          val (token, nextIndex, nextPosition) = readIdentifier(source, index, position)
          tokens += token
          index = nextIndex
          position = nextPosition

        case other =>
          errors += LexicalError(s"Caracter no reconocido: '$other'.", start)
          index += 1
          position = position.advance()
      }
    }

    tokens += Token(TokenType.EOF, "", position)

    if (errors.nonEmpty) Left(errors.toList) else Right(tokens.toList)
  }

  private def readComment(source: String, startIndex: Int, startPosition: Position): (String, Int, Position) = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder

    while (index < source.length && source.charAt(index) != '\n') {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    (builder.toString(), index, position)
  }

  private def readSlashComment(source: String, startIndex: Int, startPosition: Position): (String, Int, Position) = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder

    while (index < source.length && source.charAt(index) != '\n') {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    (builder.toString(), index, position)
  }

  private def readString(
      source: String,
      startIndex: Int,
      startPosition: Position
  ): Either[LexicalError, (Token, Int, Position)] = {
    var index = startIndex + 1
    var position = startPosition.advance()
    val builder = new StringBuilder

    while (index < source.length && source.charAt(index) != '"' && source.charAt(index) != '\n') {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    if (index >= source.length || source.charAt(index) != '"') {
      Left(LexicalError("String sin cierre.", startPosition))
    } else {
      val lexeme = "\"" + builder.toString() + "\""
      val token = Token(TokenType.StringLiteral, lexeme, startPosition, Some(builder.toString()))
      Right((token, index + 1, position.advance()))
    }
  }

  private def readNumber(source: String, startIndex: Int, startPosition: Position): (Token, Int, Position) = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder
    var hasDot = false

    while (index < source.length && {
      val current = source.charAt(index)
      current.isDigit || (!hasDot && current == '.')
    }) {
      val current = source.charAt(index)
      if (current == '.') {
        hasDot = true
      }
      builder.append(current)
      index += 1
      position = position.advance()
    }

    val lexeme = builder.toString()
    (Token(TokenType.Number, lexeme, startPosition, Some(lexeme)), index, position)
  }

  private def readIdentifier(source: String, startIndex: Int, startPosition: Position): (Token, Int, Position) = {
    var index = startIndex
    var position = startPosition
    val builder = new StringBuilder

    while (index < source.length && isIdentifierPart(source.charAt(index))) {
      builder.append(source.charAt(index))
      index += 1
      position = position.advance()
    }

    val lexeme = builder.toString()
    val tokenType = TokenType.keywords.getOrElse(lexeme, TokenType.Identifier)
    (Token(tokenType, lexeme, startPosition, Some(lexeme)), index, position)
  }

  private def isIdentifierStart(ch: Char): Boolean = ch.isLetter || ch == '_'

  private def isIdentifierPart(ch: Char): Boolean = ch.isLetterOrDigit || ch == '_'

  private def peek(source: String, index: Int): Option[Char] = {
    if (index >= 0 && index < source.length) Some(source.charAt(index)) else None
  }
}

