package services.businesslogic.statemachines

import play.api.Logger

class AutoStateMachine extends StateMachine {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина AutoStateMachine")
}
