package services.businesslogic.statemachines.typed

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import models.extractors.NoCardOrWithCard
import org.slf4j.Logger
import services.businesslogic.statemachines.typed.StateMachineTyped.StatePlatform
import services.businesslogic.statemachines.typed.StateMachineTyped.StateMachineCommand

object StateMachineTyped {
  trait StatePlatform

  trait StateMachineCommand

  case class Name(name: String) extends StateMachineCommand
  case class ProtocolExecute(message: NoCardOrWithCard) extends StateMachineCommand
  case class CardExecute(card: String) extends StateMachineCommand
  case object GetState extends StateMachineCommand


  case object Flush extends StateMachineCommand

  case object Timeout extends StateMachineCommand

}


abstract class StateMachineWraper(){

  def create(): String

}

abstract class StateMachineTyped(context: ActorContext[StateMachineCommand])
  extends AbstractBehavior[StateMachineCommand](context){

  protected val log: Logger = context.log
  log.info("Hello from StateMachineTyped!!!")

  private[this] var _name: String = ""

  private var _idnx: Int = 0

  private def setIndx(name: String): Int = (name.indexOf("["), name.indexOf("]")) match {
    case (i1, i2) if i1 >= 0 && i2 > 0 => name.substring(i1 + 1, i2).toInt
    case _ => 0
  }

  def idnx: Int = _idnx

  def name: String = _name

  def register(name: String): Unit

  def cardResponse(param: String): Unit

  def getState: Option[StatePlatform]

  def name_=(value: String): Unit = {
    _name = value
    _idnx = setIndx(value)
    register(value)
  }

  def protocolExecute(message: NoCardOrWithCard):Unit

  override def onSignal: PartialFunction[Signal, Behavior[StateMachineCommand]] =  {
    case PostStop =>
      log.info("StateMachineTyped actor stopped")
      this

  }

  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = ???
}
