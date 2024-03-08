package services.businesslogic.dispatchers.typed

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import org.slf4j.Logger
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.ParserCommand
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.oldrealisation.StateMachine


object PhisicalObjectTyped {

  trait PhisicalObjectEvent

  case class NameEvent(name: String) extends PhisicalObjectEvent
  case class PrintNameEvent(prefix: String) extends PhisicalObjectEvent
  case class TcpMessageEvent(tcpId: String, phisicalObject: String, channelName: String, message: String) extends PhisicalObjectEvent
  case class CardResponse(phisicalObject: String) extends PhisicalObjectEvent

  def apply(): Behavior[PhisicalObjectEvent] = ???

}

abstract class PhisicalObjectWraper(//parser: Parser,
                                    parser: ActorRef[ParserCommand],
                                    stateMachine: StateMachine,
                                    mainProtocolPattern: PatternInfo)   {




}


abstract class PhisicalObjectTyped(context:ActorContext[PhisicalObjectEvent],
                                   //parser: Parser,
                                   parser: ActorRef[ParserCommand],
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
