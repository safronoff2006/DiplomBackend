package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import services.businesslogic.statemachines.RailStateMachine.StateRailPlatform
import services.businesslogic.statemachines.StateMachine.StatePlatform
import services.storage.StateMachinesStorage
import utils.AtomicOption

import javax.inject.Inject

object RailStateMachine {
  case class StateRailPlatform(weight: Int) extends StatePlatform
}

class RailStateMachine  @Inject() (stateStorage: StateMachinesStorage) (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина RailStateMachine")

  override def register(name: String): Unit = stateStorage.add(name, this)

  private val state: AtomicOption[StateRailPlatform] = new AtomicOption(None)

  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: String =  message match {
      case protocolObject: RailWeight => protocolObject.weight
      case _ => "??????"
    }

    val newState = StateRailPlatform(stateData.replace('?','0').replace(' ','0').toInt)
    state.setState(Some(newState))

  }

  override def getState: Option[StatePlatform] = state.getState

  override def cardResponse(param: String): Unit = {}
}
