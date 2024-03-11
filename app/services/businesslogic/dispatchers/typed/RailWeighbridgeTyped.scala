package services.businesslogic.dispatchers.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import services.businesslogic.channelparsers.typed.ParserTyped.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.{MessageToParse, ParserCommand, SetDispatcher, SetPattern}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.storage.GlobalStorage
import services.storage.GlobalStorage.{CreateRailWeighbridgeDispatcher, MainBehaviorCommand}

import javax.inject.{Inject, Named}
import scala.util.{Failure, Success, Try}

object RailWeighbridgeTyped {

}

class RailWeighbridgeWrapper @Inject() (
                                       @Named("RailParserA") parser: ActorRef[ParserCommand],
                                        @Named("RailStateMachineA") stateMachine: ActorRef[StateMachineCommand],
                                        @Named("RailsPatternInfo") mainProtocolPattern: PatternInfo)
  extends PhisicalObjectWraper(
  parser:  ActorRef[ParserCommand],
    stateMachine: ActorRef[StateMachineCommand],
  mainProtocolPattern: PatternInfo) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан RailWeighbridgeWrapper")


  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys

  val trySys = Try {

    val sys: ActorSystem[MainBehaviorCommand] = optsys match {
      case Some(v) =>
        logger.info("Найден ActorSystem[MainBehaviorCommand]")
        v
      case None =>
        logger.error("Не найден ActorSystem[MainBehaviorCommand]")
        throw new Exception("Не найден ActorSystem[MainBehaviorCommand]")
    }

    sys
  }
  def create(): String = {

    trySys match {
      case Failure(exception) =>
        logger.error(exception.getMessage)
        ""
      case Success(sys) =>
        val id: String = java.util.UUID.randomUUID.toString
        sys ! CreateRailWeighbridgeDispatcher(parser, stateMachine, mainProtocolPattern, id)
        id
    }

  }

}

class RailWeighbridgeTyped(context: ActorContext[PhisicalObjectEvent],
                           parser:  ActorRef[ParserCommand],
                           stateMachine: ActorRef[StateMachineCommand],
                           mainProtocolPattern: PatternInfo)
  extends PhisicalObjectTyped(context,
    parser:  ActorRef[ParserCommand],
    stateMachine: ActorRef[StateMachineCommand],
    mainProtocolPattern: PatternInfo) {

  log.info(s"Создан диспетчер RailWeighbridge  ${context.self}")

  parser ! SetDispatcher(context.self)
  parser ! SetPattern(mainProtocolPattern)

  override def onMessage(msg: PhisicalObjectEvent): Behavior[PhisicalObjectEvent] = {
    msg match {
      case PhisicalObjectTyped.NameEvent(n: String) =>
        setName(n)
        log.info(s"Диспетчер именован: $name")
        stateMachine ! Name(n)
        Behaviors.same

      case PhisicalObjectTyped.PrintNameEvent(prefix) =>
        log.info(s"$prefix назначен диспетчер физических объектов $name")
        Behaviors.same

      case obj: PhisicalObjectTyped.TcpMessageEvent =>
        parser ! MessageToParse(obj.message)
        Behaviors.same

      case obj:RailWeight =>
        stateMachine ! ProtocolExecute(obj)
        Behaviors.same

      case _ =>  Behaviors.same
    }
  }
}