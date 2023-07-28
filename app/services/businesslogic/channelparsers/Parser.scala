package services.businesslogic.channelparsers

import akka.actor.ActorRef
import services.businesslogic.channelparsers.Parser.PatternInfo

object Parser {
    type PatternInfo = (String, String)
}

abstract class Parser {
  def sendToParser(message:String): Unit = ???

  protected var dispatcher: Option[ActorRef] = None

  protected var state: Int = 0

  protected var pattern: (String, String) = "" -> ""



  def setDispatcher(ref: ActorRef): Unit = dispatcher = Some(ref)

  def setPattern(p: PatternInfo): Unit = pattern = p


}
