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
import utils.{AtomicOption, EmMarineConvert}

import java.time.format.DateTimeFormatter
import javax.inject.Inject

object AutoStateMachine {
  case class Perimeters(in: Char, out: Char, left: Char, right: Char)
  case class StateAutoPlatform(perimeters: Perimeters, weight: Int, svetofor : String) extends StatePlatform


  object StateAutoPlatform {
    private class ParsePerimetersException(s: String) extends Exception(s)
    def apply(perimeters: String, weight: Int, svetofor: String): StateAutoPlatform = {
      if (!patternPerimeters.matches(perimeters))
        throw new ParsePerimetersException(s"Не верный формат периметров: $perimeters")

      val p: Perimeters = Perimeters(perimeters.charAt(0), perimeters.charAt(1),
        perimeters.charAt(2), perimeters.charAt(3))
      StateAutoPlatform(p, weight, svetofor)
    }
  }
}

class AutoStateMachine @Inject()(@Named("CardPatternName") nameCardPattern: String,
                                 stateStorage: StateMachinesStorage,
                                 @Named("ConvertEmMarine") convertEmMarine: Boolean)
                                (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина AutoStateMachine")
  logger.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.add(name, this)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)


  private def cardExecute(card: String): Unit = {
    val formatedCard = if (convertEmMarine)  EmMarineConvert.emHexToEmText(card.toUpperCase)
    else card.toUpperCase
    logger.info(s"Card: $formatedCard")
  }

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS")

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
    //val d = LocalDateTime.now
    //logger.info(s"$newState  ${formatter.format(d)}")

  }

  override def getState: Option[StatePlatform] = state.getState
}