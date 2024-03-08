package services.businesslogic.statemachines.typed

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.{NoCard, patternPerimeters}
import models.extractors.Protocol2WithCard.WithCard
import services.businesslogic.statemachines.typed.AutoStateMachineTyped.{Perimeters, StateAutoPlatform}
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.storage.StateMachinesStorage
import utils.{AtomicOption, EmMarineConvert}

import java.time.format.DateTimeFormatter

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

class AutoStateMachineTyped(context: ActorContext[StateMachineCommand],
                            nameCardPattern: String,
                            stateStorage: StateMachinesStorage,
                            convertEmMarine: Boolean,
                            cardTimeout: Long
                           ) extends StateMachineTyped(context: ActorContext[StateMachineCommand]){

  log.info("Создан актор -  стейт машина AutoStateMachine")
  log.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.addT(name, context.self)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)
  private val workedCard: AtomicOption[String] = new AtomicOption(None)

  private def processingCard(): Unit = {
    workedCard.getState match {
      case Some(card) => log.info(s"Процессинг карты $card")
      case None =>

    }
  }

  private var cardProcessingBusy = false

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



  override def cardResponse(param: String): Unit = {
    log.info(s"$name  Обработка карты завершена. Параметр $param.")
    workedCard.setState(None)
    cardProcessingBusy = false
  }

  override def getState: Option[StatePlatform] = state.getState

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
        Behaviors.same

      case ProtocolExecute(message) =>
        protocolExecute(message)
        Behaviors.same

      case _ => Behaviors.same
    }
  }
}
