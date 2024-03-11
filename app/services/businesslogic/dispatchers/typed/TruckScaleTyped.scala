package services.businesslogic.dispatchers.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import services.businesslogic.channelparsers.typed.ParserTyped._
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.storage.GlobalStorage
import services.storage.GlobalStorage.{CreateTruckScaleDispatcher, MainBehaviorCommand}

import javax.inject.{Inject, Named}
import scala.util.{Failure, Success, Try}

object TruckScaleTyped {

}


class TruckScaleWrapper @Inject()(
 @Named("AutoParserA") parser: ActorRef[ParserCommand],
 @Named("AutoStateMachineA")  stateMachine: ActorRef[StateMachineCommand] ,
 @Named("AutoMainPatternInfo") mainProtocolPattern: PatternInfo)
  extends PhisicalObjectWraper(
    parser: ActorRef[ParserCommand],
    stateMachine: ActorRef[StateMachineCommand] ,
    mainProtocolPattern: PatternInfo) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан TruckScaleWrapper")

  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys

  val trySys: Try[ActorSystem[MainBehaviorCommand]] = Try {

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
        sys ! CreateTruckScaleDispatcher(parser, stateMachine, mainProtocolPattern, id)
        id
    }


  }
}


class TruckScaleTyped(context: ActorContext[PhisicalObjectEvent],
                      parser: ActorRef[ParserCommand],
                      stateMachine: ActorRef[StateMachineCommand],
                      mainProtocolPattern: PatternInfo)
  extends PhisicalObjectTyped(context,
    parser: ActorRef[ParserCommand],
    stateMachine: ActorRef[StateMachineCommand] ,
    mainProtocolPattern: PatternInfo) {


  log.info(s"Создан диспетчер TruckScale   ${context.self}")

  parser ! SetDispatcher(context.self)
  parser ! SetPattern(mainProtocolPattern)

  override def onMessage(msg: PhisicalObjectEvent): Behavior[PhisicalObjectEvent] = {
    msg match {
      case PhisicalObjectTyped.CardResponse(phisicalObject) =>
        stateMachine ! CardRespToState(phisicalObject)
        Behaviors.same
      case PhisicalObjectTyped.NameEvent(n: String) =>
        setName(n)
        log.info(s"Диспетчер именован: $name")
        stateMachine ! Name(n)
        Behaviors.same
      case PhisicalObjectTyped.PrintNameEvent(prefix) => log.info(s"$prefix назначен диспетчер физических объектов $name")
        Behaviors.same
      case obj: PhisicalObjectTyped.TcpMessageEvent =>
        parser ! MessageToParse(obj.message)
        Behaviors.same
      case obj: NoCard =>
        stateMachine ! ProtocolExecute(obj)
        Behaviors.same
      case obj: WithCard =>
        stateMachine ! ProtocolExecute(obj)
        Behaviors.same
      case _ => Behaviors.same
    }

  }

}



