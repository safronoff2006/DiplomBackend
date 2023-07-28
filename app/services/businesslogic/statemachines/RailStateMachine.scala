package services.businesslogic.statemachines

import play.api.Logger

class RailStateMachine extends StateMachine {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создана стейт машина RailStateMachine")

}
