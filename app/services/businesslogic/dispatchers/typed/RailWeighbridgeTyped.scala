package services.businesslogic.dispatchers.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.{MessageToParse, ParserCommand, SetDispatcher, SetPattern}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.StateMachine
import services.storage.GlobalStorage
import services.storage.GlobalStorage.{CreateRailWeighbridgeDispatcher, MainBehaviorCommand}

import javax.inject.{Inject, Named}

object RailWeighbridgeTyped {

}

class RailWeighbridgeWrapper @Inject() (//@Named("RailParser") parser: Parser,
                                       @Named("RailParserA") parser: ActorRef[ParserCommand],
                                        @Named("RailStateMachine") stateMachine: StateMachine,
                                        @Named("RailsPatternInfo") mainProtocolPattern: PatternInfo)
  extends PhisicalObjectWraper(//parser: Parser,
  parser:  ActorRef[ParserCommand],
  stateMachine: StateMachine,
  mainProtocolPattern: PatternInfo) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан RailWeighbridgeWrapper")


  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys
  val sys: ActorSystem[MainBehaviorCommand] = optsys match {
    case Some(v) =>
      logger.info("Найден ActorSystem[MainBehaviorCommand]")
      v
    case None =>
      logger.error("Не найден ActorSystem[MainBehaviorCommand]")
      throw new Exception("Не найден ActorSystem[MainBehaviorCommand]")
  }

  def create(): String = {
    val id: String = java.util.UUID.randomUUID.toString
    sys ! CreateRailWeighbridgeDispatcher(parser, stateMachine, mainProtocolPattern, id)
    id
  }

}

class RailWeighbridgeTyped(context: ActorContext[PhisicalObjectEvent],
                           //parser: Parser,
                           parser:  ActorRef[ParserCommand],
                           stateMachine: StateMachine,
                           mainProtocolPattern: PatternInfo)
  extends PhisicalObjectTyped(context,
    //parser: Parser,
    parser:  ActorRef[ParserCommand],
    stateMachine: StateMachine,
    mainProtocolPattern: PatternInfo) {

  log.info(s"Создан диспетчер RailWeighbridge  ${context.self}")
  //parser.setDispatcherT(context.self)
  //parser.setPattern(mainProtocolPattern)

  parser ! SetDispatcher(context.self)
  parser ! SetPattern(mainProtocolPattern)

  override def onMessage(msg: PhisicalObjectEvent): Behavior[PhisicalObjectEvent] = {
    msg match {
      case PhisicalObjectTyped.NameEvent(n: String) =>
        setName(n)
        log.info(s"Диспетчер именован: $name")
        stateMachine.name = n
        Behaviors.same

      case PhisicalObjectTyped.PrintNameEvent(prefix) => log.info(s"$prefix назначен диспетчер физических объектов $name")
        Behaviors.same

      case obj: PhisicalObjectTyped.TcpMessageEvent =>
        //parser.sendToParser(obj.message)
        parser ! MessageToParse(obj.message)
        Behaviors.same

      case obj:RailWeight => stateMachine.protocolMessage(obj)
        Behaviors.same

      case _ =>  Behaviors.same
    }
  }
}