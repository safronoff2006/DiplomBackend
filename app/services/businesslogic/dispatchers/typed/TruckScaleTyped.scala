package services.businesslogic.dispatchers.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import services.businesslogic.channelparsers.Parser
import services.businesslogic.channelparsers.Parser.PatternInfo
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.StateMachine
import services.storage.GlobalStorage
import services.storage.GlobalStorage.{CreateTruckScaleDispatcher, MainBehaviorCommand}

import javax.inject.{Inject, Named, Singleton}

object TruckScaleTyped {

}

@Singleton
class TruckScaleWrapper @Inject()
(@Named("AutoParser") parser: Parser,
 @Named("AutoStateMachine") stateMachine: StateMachine,
 @Named("AutoMainPatternInfo") mainProtocolPattern: PatternInfo)
  extends PhisicalObjectWraper(parser: Parser,
    stateMachine: StateMachine,
    mainProtocolPattern: PatternInfo) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан TruckScaleWrapper синглтон")

  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys
  val sys = optsys match {
    case Some(v) =>
      logger.info("Найден ActorSystem[MainBehaviorCommand]")
      v
    case None =>
      logger.error("Не найден ActorSystem[MainBehaviorCommand]")
      throw new Exception("Не найден ActorSystem[MainBehaviorCommand]")
  }




  def create(): String = {
    val id: String = java.util.UUID.randomUUID.toString
    sys ! CreateTruckScaleDispatcher(parser, stateMachine, mainProtocolPattern, id)
    id
  }
}


class TruckScaleTyped(context: ActorContext[PhisicalObjectEvent], parser: Parser, stateMachine: StateMachine,
                      mainProtocolPattern: PatternInfo)
  extends PhisicalObjectTyped(context, parser: Parser, stateMachine: StateMachine, mainProtocolPattern: PatternInfo) {


  log.info(s"Создан диспетчер TruckScale   ${context.self}")
  parser.setDispatcherT(context.self)
  parser.setPattern(mainProtocolPattern)

  override def onMessage(msg: PhisicalObjectEvent): Behavior[PhisicalObjectEvent] = {
    msg match {
      case PhisicalObjectTyped.CardResponse(phisicalObject) => stateMachine.cardResponse(phisicalObject)
        Behaviors.same
      case PhisicalObjectTyped.NameEvent(n: String) =>
        setName(n)
        log.info(s"Диспетчер именован: $name")
        stateMachine.name = n
        Behaviors.same
      case PhisicalObjectTyped.PrintNameEvent(prefix) => log.info(s"$prefix назначен диспетчер физических объектов $name")
        Behaviors.same
      case obj: PhisicalObjectTyped.TcpMessageEvent => parser.sendToParser(obj.message)
        Behaviors.same
      case obj: NoCard => stateMachine.protocolMessage(obj)
        Behaviors.same
      case obj: WithCard => stateMachine.protocolMessage(obj)
        Behaviors.same
      case _ => Behaviors.same
    }

  }

}



