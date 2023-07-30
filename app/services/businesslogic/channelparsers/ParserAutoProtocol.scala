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
                compleatParseUnit(accumulator.toString)
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

  private def parseUnit2(unit: String): Unit = {
    val protocolPattern: Regex = pattern._2.r
    val correctUnit: Option[String] = Option(unit).filter(protocolPattern.matches)

    correctUnit match {
      case Some(unit) =>
        println(s"Корректная единица протокола:  $unit")
        val cardPattern: Regex = pattern._4.r
        val existCard = cardPattern.matches(unit)
         println(if (existCard) "Обнаружена карта" else "НЕ обнаружена карта")

      case None => logger.error(s"Получена не корректная единица протокола: $unit")
    }


  }


  override protected def compleatParseUnit(unit: String): Unit = {
    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2" => parseUnit2(unit)
      case _ =>
    }
  }
}
