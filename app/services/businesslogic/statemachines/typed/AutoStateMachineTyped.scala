package services.businesslogic.statemachines.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import com.google.inject.name.Named
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.{NoCard, patternPerimeters}
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import services.businesslogic.statemachines.typed.AutoStateMachineTyped.{Perimeters, StateAutoPlatform}
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.storage.GlobalStorage.{CreateAutoStateMachine, MainBehaviorCommand}
import services.storage.{GlobalStorage, StateMachinesStorage}
import utils.{AtomicOption, EmMarineConvert}

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object AutoStateMachineTyped {
  case class Perimeters(in: Char, out: Char, left: Char, right: Char)

  case class StateAutoPlatform(perimeters: Perimeters, weight: Int, svetofor: String) extends StatePlatform

  object StateAutoPlatform {
    private class ParsePerimetersException(s: String) extends Exception(s)

    def apply(perimeters: String, weight: Int, svetofor: String): StateAutoPlatform = {
      Try {
        if (!patternPerimeters.matches(perimeters)) throw new ParsePerimetersException(s"Не верный формат периметров: $perimeters")
        val p: Perimeters = Perimeters(perimeters.charAt(0), perimeters.charAt(1),
          perimeters.charAt(2), perimeters.charAt(3))
        p

      } match {
        case Failure(exception) => StateAutoPlatform(Perimeters('?', '?', '?', '?'), weight, svetofor)
        case Success(p) => StateAutoPlatform(p, weight, svetofor)
      }
    }
  }
}

class AutoStateMachineWraper @Inject()(@Named("CardPatternName") nameCardPattern: String,
                                       stateStorage: StateMachinesStorage,
                                       @Named("ConvertEmMarine") convertEmMarine: Boolean,
                                       @Named("CardTimeout") cardTimeout: Long
                                      ) extends StateMachineWraper {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан AutoStateMachineWraper")


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
        sys ! CreateAutoStateMachine(nameCardPattern, stateStorage, convertEmMarine, cardTimeout, id)
        id
    }

  }
}

class AutoStateMachineTyped(context: ActorContext[StateMachineCommand],
                            nameCardPattern: String,
                            stateStorage: StateMachinesStorage,
                            convertEmMarine: Boolean,
                            cardTimeout: Long
                           ) extends StateMachineTyped(context: ActorContext[StateMachineCommand]) {

  loger.info("Создан актор -  стейт машина AutoStateMachine")
  loger.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.addT(name, context.self)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)
  private val workedCard: AtomicOption[String] = new AtomicOption(None)

  //проанализировал - внутренний
  private def processingCard(): Unit = {
    workedCard.getState match {
      case Some(card) => loger.info(s"Процессинг карты $card")
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
      loger.warn(s"$name - Card processing Busy")
    }
  }

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS")


  //должно вызываться из кейса сообщения GetState
  override def getState: Option[StatePlatform] = state.getState

  //проанализировал - вызывается из обработки кейса сообщения  ProtocolExecute
  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: (String, String, String) = message match {
      case protocolObject: NoCard =>
        (protocolObject.perimeters, protocolObject.weight, protocolObject.svetofor)
      case protocolObject: WithCard =>
        context.self ! CardExecute(protocolObject.card)
        (protocolObject.perimeters, protocolObject.weight, protocolObject.svetofor)
      case _ => ("????", "??????", "?")
    }

    val perimeters: Perimeters = Perimeters(
      stateData._1.charAt(0),
      stateData._1.charAt(1),
      stateData._1.charAt(2),
      stateData._1.charAt(3)
    )

    val newState: StateAutoPlatform = StateAutoPlatform(perimeters, stateData._2.replace('?', '0').replace(' ', '0').toInt, stateData._3)

    state.setState(Some(newState))
    stateStorage.setHttpState(name, (newState, idnx))

  }

  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = {
    context.log.info("Create onMessage Behavior")
    msg match {
      case Name(n) =>
        name = n
        loger.info("onMessage", "Name")
        work()
      case _ => Behaviors.same
    }
  }

  private def work(): Behavior[StateMachineCommand] = Behaviors.receiveMessage[StateMachineCommand] { message =>
    context.log.info("Create work Behavior")
    message match {
      case ProtocolExecute(mess) => protocolExecute(mess)
        loger.info(s"work ProtocolExecute  $name", "ProtocolExecute")
        val optHumanName = GlobalStorage.getOptionHumanNameScaleByName(name)
        val respSend = StreamFeeder.send(ProtocolExecuteWithName(mess,name, optHumanName.getOrElse(""), idnx))
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        work()

      case CardExecute(card) =>
        loger.info(s"work CardExecute  $name", "CardExecute")
        val respSend = StreamFeeder.send(CardExecuteWithName(card, name))
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        withready()

      case GetState =>
        loger.info(s"work GetState $name    $getState", "GetState", "getState")
        work()

      case _ => Behaviors.same

    }
  }

  private def withready(): Behavior[StateMachineCommand] = Behaviors.withTimers[StateMachineCommand] { timers =>

    context.log.info("Create timeout Behavior")

    timers.startSingleTimer(Timeout, cardTimeout second)
    Behaviors.receiveMessagePartial {
      case Flush =>
        loger.info("timeout  Flush", "Flush")
        workedCard.setState(None)
        cardProcessingBusy = false
        work()

      case message@Timeout =>
        loger.warn("timeout  Timeout!!!!!!", "Timeout")
        workedCard.setState(None)
        cardProcessingBusy = false
        val respSend = StreamFeeder.send(TimeoutWithName(name))
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        work()

      case message@ProtocolExecute(mess) => protocolExecute(mess)
        loger.info(s"timeout ProtocolExecute  $name", "ProtocolExecute")
        val optHumanName = GlobalStorage.getOptionHumanNameScaleByName(name)
        val respSend = StreamFeeder.send(ProtocolExecuteWithName(mess,name, optHumanName.getOrElse(""), idnx))
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        withready()

      case GetState =>
        loger.info(s"timeout GetState $name    $getState", "GetState", "getState")
        withready()


      case message@CardRespToState(param) =>
        loger.info(s"timeout  CardRespToState  $name  Обработка карты завершена. Параметр $param.")
        val respSend = StreamFeeder.send(CardRespToStateWithName(param, name))
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        workedCard.setState(None)
        cardProcessingBusy = false
        work()

      case _ => Behaviors.same
    }

  }


}
