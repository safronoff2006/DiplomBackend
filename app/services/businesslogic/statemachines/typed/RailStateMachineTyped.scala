package services.businesslogic.statemachines.typed

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.extractors.NoCardOrWithCard
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import services.businesslogic.statemachines.typed.RailStateMachineTyped.StateRailPlatform
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.storage.GlobalStorage.MainBehaviorCommand
import services.storage.{GlobalStorage, StateMachinesStorage}
import utils.AtomicOption
import services.storage.GlobalStorage.CreateRailStateMachine

import javax.inject.Inject

object RailStateMachineTyped {
  case class StateRailPlatform(weight: Int) extends StatePlatform
}

class RailStateMachineWraper @Inject()(stateStorage: StateMachinesStorage) extends StateMachineWraper {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан RailStateMachineWraper")

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
    sys ! CreateRailStateMachine(stateStorage, id)
    id
  }
}


class  RailStateMachineTyped(context: ActorContext[StateMachineCommand],stateStorage: StateMachinesStorage)
  extends StateMachineTyped(context: ActorContext[StateMachineCommand]) {

  log.info("Создан актор -  стейт машина RailStateMachineTyped")
  private val state: AtomicOption[StateRailPlatform] = new AtomicOption(None)

  override def register(name: String): Unit = stateStorage.addT(name, context.self)

  override def cardResponse(param: String): Unit = {}

  override def getState: Option[StatePlatform]  = state.getState

  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = {
    msg match {
      case Name(n) =>
        name = n
        log.info("onMessage","Name")
        Behaviors.same

      case ProtocolExecute(message) => protocolExecute(message)
        log.info("onMessage", "ProtocolExecute")
        Behaviors.same

      case GetState =>
        log.info("onMessage", "GetState", getState)
        Behaviors.same
    }
  }

  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: String = message match {
      case protocolObject: RailWeight => protocolObject.weight
      case _ => "??????"
    }

    val newState = StateRailPlatform(stateData.replace('?', '0').replace(' ', '0').toInt)
    state.setState(Some(newState))


  }
}