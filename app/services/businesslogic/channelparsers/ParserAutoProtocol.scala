package services.businesslogic.channelparsers

import play.api.Logger

class ParserAutoProtocol  extends Parser {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола AutoMain")

  override def sendToParser(message: String): Unit = {
    println(message)
  }
}
