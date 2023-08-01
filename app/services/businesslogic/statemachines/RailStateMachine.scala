package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import play.api.Logger

import javax.inject.Inject

class RailStateMachine  @Inject() (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина RailStateMachine")

  override def protocolExecute(message: NoCardOrWithCard): Unit = {

  }
}
