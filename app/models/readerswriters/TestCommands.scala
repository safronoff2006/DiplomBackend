package models.readerswriters

import play.api.Logger



object TestCommands {


  val logger: Logger = Logger(this.getClass)
  class ParseJsValueToTestCommandException(s: String) extends Exception(s)

}
