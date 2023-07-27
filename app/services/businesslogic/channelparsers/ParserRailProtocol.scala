package services.businesslogic.channelparsers

import play.api.Logger

class ParserRailProtocol extends Parser {

  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола RailsMain")

  override def sendToParser(message: String): Unit = {
    println(message)
  }
}
