package services.businesslogic.channelparsers

import executioncontexts.CustomBlockingExecutionContext
import play.api.Logger
import services.businesslogic.channelparsers.Parser.PatternInfo

import javax.inject.Inject
import scala.util.matching.Regex

class ParserAutoProtocol @Inject()(implicit ex: CustomBlockingExecutionContext) extends Parser() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создан парсер протокола AutoMain")

  private var protocolPrefixes: List[Char] = List()
  private var protocolSuffixes: List[Char] = List()

  private def parseProtocol2EmMarine(message: String): Unit = {
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
                compleatParseUnit(accumulator.toString)
                clearState()
            }
        }
    }
  }

  override def setPattern(p: PatternInfo): Unit = {
    super.setPattern(p)
    protocolPrefixes = p._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN" => List('v', 'V')
      case _ => List('<')
    }

    protocolSuffixes = p._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN" => List('.')
      case _ => List('>')
    }

  }

  override protected def parse(message: String): Unit = {
    logger.info(s"Парсинг сообщения  $message")
    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN" => parseProtocol2EmMarine(message)
      case _ =>
    }

  }

  private def parseUnit2EmMarine(unit: String): Unit = {
    val protocolPattern: Regex = pattern._2.r
    val correctUnit: Option[String] = Option(unit).filter(protocolPattern.matches)

    correctUnit match {
      case Some(unit) => println(s"Корректная единица протокола:  $unit")
      case None => println("Не корректная единица протокола")
    }


  }


  override protected def compleatParseUnit(unit: String): Unit = {
    logger.info(s"Единица протокола: $unit")
    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN" => parseUnit2EmMarine(unit)
      case _ =>
    }
  }
}
