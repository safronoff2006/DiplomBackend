package services.businesslogic.channelparsers

import play.api.Logger

class ParserAutoProtocol  extends Parser {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола AutoMain")



  private def parseProtocol2EmMarine(message: String): Unit = {
    println(message)
  }


  override def sendToParser(message: String): Unit = {

    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN" => parseProtocol2EmMarine(message)
      case _ =>
    }

  }
}
