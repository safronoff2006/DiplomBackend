package services.businesslogic.channelparsers.typed

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import org.slf4j.Logger
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.ParserCommand
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent


object ParserTyped {
  trait ParserCommand
  case class SetPattern(p: PatternInfo) extends ParserCommand
  case class SetDispatcher(ref: ActorRef[PhisicalObjectEvent]) extends ParserCommand
  case class MessageToParse(message: String) extends ParserCommand

}

abstract class ParserWraper(){

  def create(): String

}

abstract class ParserTyped(context: ActorContext[ParserCommand]) extends AbstractBehavior[ParserCommand](context) {

  protected val log: Logger = context.log
  log.info("Hello from  ParserTyped!!!")

  private var dispatcherT: Option[ActorRef[PhisicalObjectEvent]] = None

  protected var state: Int = 0

  protected val accumulator: StringBuilder = new StringBuilder("")

  override def onMessage(msg: ParserCommand): Behavior[ParserCommand] = ???

  protected var pattern: PatternInfo = ("","","","")

  def setDispatcherT(ref: ActorRef[PhisicalObjectEvent]): Unit = dispatcherT = Some(ref)

  def setPattern(p: PatternInfo): Unit = pattern = p

  def getDispatcherT: Option[ActorRef[PhisicalObjectEvent]] = dispatcherT

  protected val maxUnitLength = 100

  protected var unitCount: Int = 0

  protected def clearState(): Unit = {
    accumulator.clear()
    unitCount = 0
    state = 0
  }

  protected def parse(message:String): Unit

  override def onSignal: PartialFunction[Signal, Behavior[ParserCommand]] = {
    case PostStop =>
      log.info("ParserTyped actor stopped")
      this
  }
}


