package services.businesslogic.channelparsers.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.extractors.{NoCardOrWithCard, ProtocolRail}
import play.api.Logger
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.{MessageToParse, ParserCommand, SetDispatcher, SetPattern}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.storage.GlobalStorage
import services.storage.GlobalStorage.{CreateRailProtocolParser, MainBehaviorCommand}

object ParserRailProtocolTyped {

}

class ParserRailProtokolWraper() extends ParserWraper {
  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан ParserRailProtokolWraper")

  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys
  val sys: ActorSystem[MainBehaviorCommand] = optsys match {
    case Some(v) =>
      logger.info("Найден ActorSystem[MainBehaviorCommand]")
      v
    case None =>
      logger.error("Не найден ActorSystem[MainBehaviorCommand]")
      throw new Exception("Не найден ActorSystem[MainBehaviorCommand]")
  }

  override def create(): String = {
    val id: String = java.util.UUID.randomUUID.toString
    sys ! CreateRailProtocolParser(id)
    id
  }

}


class ParserRailProtocolTyped(context: ActorContext[ParserCommand]) extends ParserTyped(context: ActorContext[ParserCommand]) {
  log.info("Создан актор -  парсер протокола RailsMain")

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

  private def sendProtocolObjectToDispatcher(protocolObj: NoCardOrWithCard with PhisicalObjectEvent): Unit = {
    getDispatcherT match {
      case Some(dispatcherRef) => dispatcherRef ! protocolObj
      case None => log.error("Не заполнен диспетчер физического объекта")
    }
  }

  private def compleatParseRUnit(unit: String): Unit = {
    unit match {
      case ProtocolRail(protocolObj) => sendProtocolObjectToDispatcher(protocolObj)
      case _ => log.error(s"Единица протокола $unit не соответствует ни какому протоколу")
    }
  }

  override protected def parse(message: String): Unit = {
    pattern._1 match {
      case "SCALE_DATA_PATTERN_RAIL_PROTOCOL" => parseProtokolR(message)
      case _ =>
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

      case MessageToParse(m)  => parse(m)
      Behaviors.same
    }
  }
}
