package services.businesslogic.dispatchers.notyped

import akka.actor.Actor
import akka.event.{Logging, LoggingAdapter}
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.channelparsers.oldrealisation.Parser
import services.businesslogic.statemachines.oldrealisation.StateMachine


object PhisicalObject {

  case class NameEvent(name: String)
  case class PrintNameEvent(prefix:String)
  case class TcpMessageEvent(tcpId: String, phisicalObject: String, channelName: String, message: String)
  case class CardResponse(phisicalObject: String)
}
abstract class PhisicalObject(parser: Parser,
                              stateMachine: StateMachine,
                              mainProtocolPattern: PatternInfo) extends Actor {
  protected val log: LoggingAdapter = Logging(context.system, this)

  def receive: Receive = ???

  var name: String = ""

  def setName(name:String): String = {
    this.name = name
    this.name
  }
}
