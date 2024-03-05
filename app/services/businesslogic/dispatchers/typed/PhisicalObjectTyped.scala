package services.businesslogic.dispatchers.typed

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.actor.typed.{Behavior, PostStop, Signal}
import org.slf4j.Logger
import services.businesslogic.channelparsers.Parser
import services.businesslogic.channelparsers.Parser.PatternInfo
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.StateMachine



object PhisicalObjectTyped {

  trait PhisicalObjectEvent

  case class NameEvent(name: String) extends PhisicalObjectEvent
  case class PrintNameEvent(prefix: String) extends PhisicalObjectEvent
  case class TcpMessageEvent(tcpId: String, phisicalObject: String, channelName: String, message: String) extends PhisicalObjectEvent
  case class CardResponse(phisicalObject: String) extends PhisicalObjectEvent

  def apply(): Behavior[PhisicalObjectEvent] = ???

}

abstract class PhisicalObjectWraper(parser: Parser,
                                    stateMachine: StateMachine,
                                    mainProtocolPattern: PatternInfo)   {

}


abstract class PhisicalObjectTyped(context:ActorContext[PhisicalObjectEvent],  parser: Parser,
                                   stateMachine: StateMachine,
                                   mainProtocolPattern: PatternInfo)  extends  AbstractBehavior[PhisicalObjectEvent](context) {


protected val log: Logger =  context.log
log.info("Hello from PhisicalObjectTyped !!!")

  override def onSignal: PartialFunction[Signal, Behavior[PhisicalObjectEvent]] = {
    case PostStop =>
      log.info("PhisicalObjectTyped actor  stopped")
      this
  }

  override def onMessage(msg: PhisicalObjectEvent): Behavior[PhisicalObjectEvent] = ???

  var name: String = ""

  def setName(name: String): String = {
    this.name = name
    this.name
  }

}
