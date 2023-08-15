package services.businesslogic.channelparsers

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.{NoCardOrWithCard, ProtocolRail}
import play.api.Logger
import services.businesslogic.channelparsers.Parser.PatternInfo

import javax.inject.Inject

class ParserRailProtocol @Inject() (implicit ex:CustomBlockingExecutionContext) extends Parser() {

  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола RailsMain")

  private var protocolPrefixes: List[Char] = List()
  private var protocolSuffixes: List[Char] = List()


  private def parseProtokolR(message: String): Unit = {
    message.foreach {
      ch =>
        state match {
          case 0 =>
            if (protocolPrefixes.contains(ch)) {
              accumulator += ch
              unitCount += 1
              state = 1
            }
          case 1 =>
            accumulator += ch
            unitCount += 1
            if (unitCount >= maxUnitLength) clearState()
            else if (protocolSuffixes.contains(ch)) {
              compleatParseRUnit(accumulator.toString)
              clearState()
            }
        }
    }
  }


  override def setPattern(p: PatternInfo): Unit = {
    super.setPattern(p)
    protocolPrefixes = p._1 match {
      case "SCALE_DATA_PATTERN_RAIL_PROTOCOL" => List('=')
      case _ => List('<')
    }

    protocolSuffixes = p._1 match {
      case "SCALE_DATA_PATTERN_RAIL_PROTOCOL" => List('.')
      case _ => List('<')
    }
  }

  override protected def parse(message: String): Unit = {
    logger.info(s"Парсинг сообщения  $message" )
    pattern._1 match {
      case "SCALE_DATA_PATTERN_RAIL_PROTOCOL" => parseProtokolR(message)
      case _ =>
    }
  }

  private def sendProtocolObjectToDispatcher(protocolObj: NoCardOrWithCard): Unit = {
    getDispatcher match {
      case Some(dispatcherRef) => dispatcherRef ! protocolObj
      case None => logger.error("Не заполнен диспетчер физического объекта")
    }
  }

  private def compleatParseRUnit(unit: String): Unit = {
    unit match {
      case ProtocolRail(protocolObj) => sendProtocolObjectToDispatcher(protocolObj)
      case _ => logger.error(s"Единица протокола $unit не соответствует ни какому протоколу")
    }
  }
}
