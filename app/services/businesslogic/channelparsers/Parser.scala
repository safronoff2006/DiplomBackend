package services.businesslogic.channelparsers

import akka.actor.ActorRef

abstract class Parser {
  def sendToParser(message:String): Unit = ???

  protected var dispatcher: Option[ActorRef] = None

  protected var state: Int = 0

  def setDispatcher(ref: ActorRef): Unit = dispatcher = Some(ref)

}
