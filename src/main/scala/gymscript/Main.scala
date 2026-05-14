package gymscript

import gymscript.cli.CliApp

object Main {
  def main(args: Array[String]): Unit = {
    val exitCode = new CliApp().run(args)
    if (exitCode != 0) {
      sys.exit(exitCode)
    }
  }
}

