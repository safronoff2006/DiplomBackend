package services.businesslogic.channelparsers

import executioncontexts.CustomBlockingExecutionContext
import play.api.Logger

import javax.inject.Inject

class ParserRailProtocol @Inject() (implicit ex:CustomBlockingExecutionContext) extends Parser() {

  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола RailsMain")

  override protected def parse(message: String): Unit = {
    logger.info(s"Парсинг сообщения  $message" )
    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL1" =>
      case _ =>
    }
  }


}
