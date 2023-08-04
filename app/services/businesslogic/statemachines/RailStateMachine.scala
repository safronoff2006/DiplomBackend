package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import play.api.Logger
import services.businesslogic.statemachines.StateMachine.StatePlatform
import services.storage.StateMachinesStorage

import javax.inject.Inject

class RailStateMachine  @Inject() (stateStorage: StateMachinesStorage) (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина RailStateMachine")

  override def register(name: String): Unit = stateStorage.add(name, this)

  override def protocolExecute(message: NoCardOrWithCard): Unit = {

  }

  override def getState: Option[StatePlatform] = None
}
