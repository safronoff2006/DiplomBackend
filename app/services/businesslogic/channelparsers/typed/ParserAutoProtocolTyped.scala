package services.businesslogic.channelparsers.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.extractors.{NoCardOrWithCard, Protocol2NoCard, Protocol2WithCard}
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.{MessageToParse, ParserCommand, SetDispatcher, SetPattern}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.storage.GlobalStorage
import services.storage.GlobalStorage.{CreateAutoProtocolParser, MainBehaviorCommand}

import scala.util.{Failure, Success, Try}

object ParserAutoProtocolTyped {

}

class ParserAutoProtocolWraper() extends ParserWraper {
  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан ParserAutoProtocolWraper")

  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys
  val trySys: Try[ActorSystem[MainBehaviorCommand]] = Try {

    val sys: ActorSystem[MainBehaviorCommand] = optsys match {
      case Some(v) =>
        logger.info("Найден ActorSystem[MainBehaviorCommand]")
        v
      case None =>
        logger.error("Не найден ActorSystem[MainBehaviorCommand]")
        throw new Exception("Не найден ActorSystem[MainBehaviorCommand]")
    }

    sys

  }

  override def create(): String = {

    trySys match {
      case Failure(exception) =>
        logger.error(exception.getMessage)
        ""
      case Success(sys) =>
        val id: String = java.util.UUID.randomUUID.toString
        sys ! CreateAutoProtocolParser(id)
        id
    }

  }
}

class ParserAutoProtocolTyped(context: ActorContext[ParserCommand]) extends ParserTyped(context: ActorContext[ParserCommand]) {

  log.info("Создан актор -  парсер протокола AutoMain")

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

  private def compleatParse2Unit(unit: String): Unit = {
    unit match {
      case Protocol2NoCard(protocolObj) => sendProtocolObjectToDispatcher(protocolObj)
      case Protocol2WithCard(protocolObj) =>
        protocolObj match {
          case WithCard(_, _, _, _, _, "M", _) => sendProtocolObjectToDispatcher(protocolObj)
          case WithCard(_, _, _, _, _, "Q", _) =>
            log.warn(s"Поступил не поддерживаемый системой QR-код: $protocolObj")
          case _ =>
        }
      case _ => log.error(s"Единица протокола $unit не соответствует ни какому протоколу")
    }
  }

  private def sendProtocolObjectToDispatcher(protocolObj: NoCardOrWithCard with PhisicalObjectEvent): Unit = {
    getDispatcherT match {
      case Some(dispatcherRef) => dispatcherRef ! protocolObj
      case None => log.error("Не заполнен диспетчер физического объекта")
    }
  }

  override protected def parse(message: String): Unit = {
    pattern._1 match {
      case "SCALE_DATA_PATTERN_PROTOCOL2" => parseProtocol2(message)
      case _ =>
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

  override def onMessage(msg: ParserCommand): Behavior[ParserCommand] = {
    msg match {
      case SetPattern(p) =>
        setPattern(p)
        Behaviors.same

      case SetDispatcher(ref) =>
        setDispatcherT(ref)
        Behaviors.same

      case MessageToParse(m) => parse(m)
      Behaviors.same
    }
  }
}
