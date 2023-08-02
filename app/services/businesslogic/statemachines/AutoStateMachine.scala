package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger

import javax.inject.Inject

object AutoStateMachine {
  case class StateAutoPlatform()
}

class AutoStateMachine  @Inject() (implicit ex: CustomBlockingExecutionContext) extends StateMachine() {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина AutoStateMachine")




  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    message match {
      case protocolObject: NoCard => println(s"NO CARD: $protocolObject")
      case protocolObject: WithCard => println(s"WITH CARD: $protocolObject")
      case _ =>
    }
  }

}