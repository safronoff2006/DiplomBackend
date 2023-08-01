package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import play.api.Logger

import javax.inject.Inject

class AutoStateMachine  @Inject() (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина AutoStateMachine")

  override def protocolExecute(message: NoCardOrWithCard): Unit = {

  }
}
