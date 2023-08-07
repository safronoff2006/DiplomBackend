package services.businesslogic.statemachines

import com.google.inject.name.Named
import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.{NoCard, patternPerimeters}
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger

import services.businesslogic.statemachines.AutoStateMachine.{Perimeters, StateAutoPlatform}
import services.businesslogic.statemachines.StateMachine.StatePlatform
import services.storage.StateMachinesStorage
import utils.AtomicOption

import javax.inject.Inject

object AutoStateMachine {
  case class Perimeters(in: Char, out: Char, left: Char, right: Char)
  case class StateAutoPlatform(perimeters: Perimeters, weight: Int) extends StatePlatform
  object StateAutoPlatform {
    class ParsePerimetersException(s: String) extends Exception(s)
    def apply(perimeters: String, weight: Int): StateAutoPlatform = {
      if (!patternPerimeters.matches(perimeters))
        throw new ParsePerimetersException(s"Не верный формат периметров: $perimeters")

      val p: Perimeters = Perimeters(perimeters.charAt(0), perimeters.charAt(1),
        perimeters.charAt(2), perimeters.charAt(3))
      StateAutoPlatform(p, weight)
    }
  }
}

class AutoStateMachine @Inject()(@Named("CardPatternName") nameCardPattern: String,
                                 stateStorage: StateMachinesStorage)
                                (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина AutoStateMachine")
  logger.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.add(name, this)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)


  private def cardExecute(card: String): Unit = {

  }


  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: (String, String) = message match {
      case protocolObject: NoCard =>
        (protocolObject.perimeters, protocolObject.weight)
      case protocolObject: WithCard =>
        cardExecute(protocolObject.card)
        (protocolObject.perimeters, protocolObject.weight)
      case _ => ("????", "??????")
    }

    val perimeters: Perimeters = Perimeters(
      stateData._1.charAt(0),
      stateData._1.charAt(1),
      stateData._1.charAt(2),
      stateData._1.charAt(3)
    )

    val newState = StateAutoPlatform(perimeters, stateData._2.replace('?', '0').replace(' ', '0').toInt)

    state.setState(Some(newState))

    println(newState)

  }

  override def getState: Option[StatePlatform] = state.getState
}