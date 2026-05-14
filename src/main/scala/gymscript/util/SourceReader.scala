package gymscript.util

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths }

import scala.util.Try

object SourceReader {
  def read(path: String): Either[String, String] = {
    Try(Files.readString(Paths.get(path), StandardCharsets.UTF_8))
      .toEither
      .left
      .map(error => s"No fue posible leer '$path': ${error.getMessage}")
  }
}

