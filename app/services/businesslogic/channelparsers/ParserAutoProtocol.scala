package services.businesslogic.channelparsers

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.Protocol2WithCard.WithCard
import models.extractors.{NoCardOrWithCard, Protocol2NoCard, Protocol2WithCard}
import play.api.Logger
import services.businesslogic.channelparsers.Parser.PatternInfo

import javax.inject.Inject

class ParserAutoProtocol @Inject()(implicit ex: CustomBlockingExecutionContext) extends Parser() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола AutoMain")

  private var protocolPrefixes: List[Char] = List()
  private var protocolSuffixes: List[Char] = List()

  private var patternCard = ""

  private def parseProtocol2(message: String): Unit = {
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
                compleatParse2Unit(accumulator.toString)
                clearState()
            }
        }
    }
  }

  override def setPattern(p: PatternInfo): Unit = {
    super.setPattern(p)
    protocolPrefixes = p._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2" => List('v', 'V')
      case _ => List('<')
    }

    protocolSuffixes = p._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2" => List('.')
      case _ => List('>')
    }


  }

  override protected def parse(message: String): Unit = {
    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2" => parseProtocol2(message)
      case _ =>
    }
  }

  private def sendProtocolObjectToDispatcher(protocolObj: NoCardOrWithCard): Unit = {
    getDispatcher match {
      case Some(dispatcherRef) => dispatcherRef ! protocolObj
      case None => logger.error("Не заполнен диспетчер физического объекта")
    }
  }


  private def compleatParse2Unit(unit: String): Unit = {
       unit match {
         case Protocol2NoCard(protocolObj) => sendProtocolObjectToDispatcher(protocolObj)
         case Protocol2WithCard(protocolObj) =>
           protocolObj match {
             case WithCard(_, _, _, _, _, "M") => sendProtocolObjectToDispatcher(protocolObj)
             case WithCard(_, _, _, _, _, "Q") =>
               logger.warn(s"Поступил не поддерживаемый системой QR-код: $protocolObj")
             case _ =>
           }
         case _ => logger.error(s"Единица протокола $unit не соответствует ни какому протоколу")
       }
  }
}
