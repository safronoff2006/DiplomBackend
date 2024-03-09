package services.businesslogic.statemachines.oldrealisation

import com.google.inject.name.Named
import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.{NoCard, patternPerimeters}
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import StateMachine.StatePlatform
import services.businesslogic.statemachines.oldrealisation.AutoStateMachine.{Perimeters, StateAutoPlatform}
import services.storage.StateMachinesStorage
import utils.{AtomicOption, EmMarineConvert}

import java.time.format.DateTimeFormatter
import java.util.concurrent.{Exchanger, TimeUnit, TimeoutException}
import javax.inject.Inject



object AutoStateMachine {
  case class Perimeters(in: Char, out: Char, left: Char, right: Char)

  case class StateAutoPlatform(perimeters: Perimeters, weight: Int, svetofor: String) extends StatePlatform


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
                                 @Named("ConvertEmMarine") convertEmMarine: Boolean,
                                 @Named("CardTimeout") cardTimeout: Long)
                                (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина AutoStateMachine")
  logger.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.add(name, this)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)

  private val workedCard: AtomicOption[String] = new AtomicOption(None)
  private val workedExchanger: AtomicOption[Exchanger[String]] = new AtomicOption(None)

//проанализировал - внутренний
  private def processingCard(): Unit = {
    workedCard.getState match {
      case Some(card) =>  logger.info(s"Процессинг карты $card")
      case None =>

    }
  }

  //проанализировал - внутренний
  private def cardExecute(card: String): Unit = {
    val formatedCard = if (convertEmMarine) EmMarineConvert.emHexToEmText(card.toUpperCase)
    else card.toUpperCase

    if (workedExchanger.getState.isEmpty) {
      workedCard.setState(Some(formatedCard))
      ex.execute(() => {
        processingCard()
        val exchanger = new Exchanger[String]
        workedExchanger.setState(Some(exchanger))
        try {
          exchanger.exchange(formatedCard, cardTimeout, TimeUnit.MILLISECONDS)
          workedCard.setState(None)
          workedExchanger.setState(None)
          logger.info(s"$name  Обработка карты завершена")
        } catch {
          case e: InterruptedException =>
            workedCard.setState(None)
            workedExchanger.setState(None)
            logger.warn(s"$name  Прерывание эксченджера процессинга карты")
          case e: TimeoutException =>
            workedCard.setState(None)
            workedExchanger.setState(None)
            logger.warn(s"$name  Таймаут ответа на  карту ( $cardTimeout миллисекунд )")
        }
      })
    }
  }





  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS")

  //проанализировал - вызывается из обработчика очереди сообщений
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

  //проанализировал - вызывается
  //1) из  controllers.MainController.getState(name:String)
  //2) из jsonStatesOfListStates  в controllers.MainController
  override def getState: Option[StatePlatform] = state.getState

  //должно вызываться из внешнего по отношению к сервису потока
  override def cardResponse(param: String): Unit = {
    workedExchanger.getState match {
      case Some(exchanger) => exchanger.exchange(param)
      case None =>
    }
  }
}