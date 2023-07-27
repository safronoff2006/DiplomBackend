package services.businesslogic.dispatchers

import akka.actor.Actor
import akka.event.{Logging, LoggingAdapter}
import services.businesslogic.channelparsers.Parser


object PhisicalObject {
  case class NameEvent(name: String)
  case class PrintNameEvent(prefix:String)
  case class TcpMessageEvent(tcpId: String, phisicalObject: String, channelName: String, message: String)
}


abstract class PhisicalObject(parser: Parser) extends Actor {
  protected val log: LoggingAdapter = Logging(context.system, this)

  def receive: Receive = ???

  var name: String = ""

  def setName(name:String): String = {
    this.name = name
    this.name
  }
}
