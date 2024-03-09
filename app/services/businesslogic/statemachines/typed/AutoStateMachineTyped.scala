package services.businesslogic.statemachines.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.google.inject.name.Named
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.{NoCard, patternPerimeters}
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import services.businesslogic.statemachines.typed.AutoStateMachineTyped.{Perimeters, StateAutoPlatform}
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.storage.GlobalStorage.MainBehaviorCommand
import services.storage.{GlobalStorage, StateMachinesStorage}
import utils.{AtomicOption, EmMarineConvert}

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import services.storage.GlobalStorage.CreateAutoStateMachine

object AutoStateMachineTyped {
  case class Perimeters(in: Char, out: Char, left: Char, right: Char)

  case class StateAutoPlatform(perimeters: Perimeters, weight: Int, svetofor: String) extends StatePlatform

  object StateAutoPlatform {
    private class ParsePerimetersException(s: String) extends Exception(s)

    def apply(perimeters: String, weight: Int, svetofor: String): StateAutoPlatform = {
      if (!patternPerimeters.matches(perimeters)) throw new ParsePerimetersException(s"Не верный формат периметров: $perimeters")
      val p: Perimeters = Perimeters(perimeters.charAt(0), perimeters.charAt(1),
        perimeters.charAt(2), perimeters.charAt(3))
      StateAutoPlatform(p, weight, svetofor)
    }
  }
}

class AutoStateMachineWraper @Inject()( @Named("CardPatternName") nameCardPattern: String,
                                        stateStorage: StateMachinesStorage,
                                        @Named("ConvertEmMarine") convertEmMarine: Boolean,
                                        @Named("CardTimeout") cardTimeout: Long
                                      ) extends StateMachineWraper {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан AutoStateMachineWraper")

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
    sys ! CreateAutoStateMachine(nameCardPattern,  stateStorage, convertEmMarine,  cardTimeout, id)
    id
  }
}

class AutoStateMachineTyped(context: ActorContext[StateMachineCommand],
                            nameCardPattern: String,
                            stateStorage: StateMachinesStorage,
                            convertEmMarine: Boolean,
                            cardTimeout: Long
                           ) extends StateMachineTyped(context: ActorContext[StateMachineCommand]) {

  log.info("Создан актор -  стейт машина AutoStateMachine")
  log.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.addT(name, context.self)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)
  private val workedCard: AtomicOption[String] = new AtomicOption(None)

  //проанализировал - внутренний
  private def processingCard(): Unit = {
    workedCard.getState match {
      case Some(card) => log.info(s"Процессинг карты $card")
      case None =>

    }
  }

  private var cardProcessingBusy = false

  //проанализировал - внутренний
  private def cardExecute(card: String): Unit = {
    val formatedCard = if (convertEmMarine) EmMarineConvert.emHexToEmText(card.toUpperCase)
    else card.toUpperCase

    if (!cardProcessingBusy) {
      cardProcessingBusy = true
      workedCard.setState(Some(formatedCard))
      processingCard()
    } else {
      log.warn(s"$name - Card processing Busy")
    }
  }

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS")


  //должно вызываться из кейса сообщения СardResponse
  override def cardResponse(param: String): Unit = {
    log.info(s"$name  Обработка карты завершена. Параметр $param.")
    context.self ! Flush
  }

  //должно вызываться из кейса сообщения GetState
  override def getState: Option[StatePlatform] = state.getState

  //проанализировал - вызывается из обработки кейса сообщения  ProtocolExecute
  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: (String, String, String) = message match {
      case protocolObject: NoCard =>
        (protocolObject.perimeters, protocolObject.weight, protocolObject.svetofor)
      case protocolObject: WithCard =>
        cardExecute(protocolObject.card)
        (protocolObject.perimeters, protocolObject.weight, protocolObject.svetofor)
      case _ => ("????", "??????", "?")
    }

    val perimeters: Perimeters = Perimeters(
      stateData._1.charAt(0),
      stateData._1.charAt(1),
      stateData._1.charAt(2),
      stateData._1.charAt(3)
    )

    val newState = StateAutoPlatform(perimeters, stateData._2.replace('?', '0').replace(' ', '0').toInt, stateData._3)

    state.setState(Some(newState))


  }

  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = {
    msg match {
      case Name(n) =>
        name = n
        log.info("onMessage","Name")
        work()
      case _ => Behaviors.same
    }
  }

  private def work(): Behavior[StateMachineCommand] = Behaviors.receiveMessage[StateMachineCommand] {
    case ProtocolExecute(message) => protocolExecute(message)
      log.info("work", "ProtocolExecute")
      work()

    case CardExecute(card) =>
      log.info("work", "CardExecute")
      timeout()

    case GetState =>
      log.info("work", "GetState", getState)
      work()

    case _ => Behaviors.unhandled
  }

  private def timeout(): Behavior[StateMachineCommand] = Behaviors.withTimers[StateMachineCommand] { timers =>
    timers.startSingleTimer(Timeout, 5 second)
    Behaviors.receiveMessagePartial {
      case Flush =>
        log.info("timeout","Flush")
        workedCard.setState(None)
        cardProcessingBusy = false
        work()

      case Timeout =>
        log.info("timeout","Timeout")
        workedCard.setState(None)
        cardProcessingBusy = false
        work()

      case CardExecute(card) =>  cardExecute(card)
        log.info("timeout","CardExecute")
        timeout()

      case _ => Behaviors.unhandled
    }

  }
}
