package services.businesslogic.dispatchers.typed

import akka.actor.Actor
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard
import services.businesslogic.channelparsers.Parser
import services.businesslogic.channelparsers.Parser.PatternInfo
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.StateMachine

import javax.inject.{Inject, Named}

object TruckScaleTyped {
  trait BuildFactory {
    def apply(): Actor
  }
}

class TruckScaleWrapper @Inject()
(@Named("AutoParser") parser: Parser,
 @Named("AutoStateMachine") stateMachine: StateMachine,
 @Named("AutoMainPatternInfo") mainProtocolPattern: PatternInfo) extends PhisicalObjectWraper(parser: Parser,
  stateMachine: StateMachine,
  mainProtocolPattern: PatternInfo) {
  Behaviors.setup[PhisicalObjectEvent] { ctx =>
      new TruckScaleTyped(ctx, parser, stateMachine, mainProtocolPattern)
  }


}


class TruckScaleTyped(context: ActorContext[PhisicalObjectEvent], parser: Parser, stateMachine: StateMachine,
                      mainProtocolPattern: PatternInfo) extends PhisicalObjectTyped(context, parser: Parser, stateMachine: StateMachine, mainProtocolPattern: PatternInfo) {


  log.info("Создан диспетчер TruckScale")
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
      case PhisicalObjectTyped.PrintNameEvent(prefix) =>    log.info(s"$prefix назначен диспетчер физических объектов $name")
        Behaviors.same
      case obj:PhisicalObjectTyped.TcpMessageEvent => parser.sendToParser(obj.message)
        Behaviors.same
      case obj: NoCard => stateMachine.protocolMessage(obj)
        Behaviors.same
      case obj: WithCard => stateMachine.protocolMessage(obj)
        Behaviors.same
      case _ => Behaviors.same
    }

  }

}
