package services.businesslogic.dispatchers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard
import play.api.Logger
import services.businesslogic.channelparsers.Parser
import services.businesslogic.channelparsers.Parser.PatternInfo

import javax.inject.{Inject, Named, Singleton}
import services.businesslogic.dispatchers.PhisicalObject._
import services.businesslogic.statemachines.StateMachine

object TruckScale {
  trait BuildFactory {
    def apply(): Actor
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }
}

class  TruckScale @Inject()(@Named("AutoParser") parser: Parser,
                            @Named("AutoStateMachine") stateMachine: StateMachine,
                            @Named("AutoMainPatternInfo") mainProtocolPattern: PatternInfo)

  extends PhisicalObject(parser:Parser, stateMachine: StateMachine, mainProtocolPattern: PatternInfo) with Actor  {

  log.info("Создан диспетчер TruckScale")
  parser.setDispatcher(self)
  parser.setPattern(mainProtocolPattern)

  override def receive: Receive = {
    case NameEvent(n: String) =>
      setName(n)
      log.info(s"Диспетчер именован: $name")
      stateMachine.name = n
    case PrintNameEvent(prefix) =>    log.info(s"$prefix назначен диспетчер физических объектов $name")
    case obj:TcpMessageEvent =>
      parser.sendToParser(obj.message)
    case obj:NoCard => stateMachine.protocolMessage(obj)
    case obj:WithCard => stateMachine.protocolMessage(obj)
    case _ =>
  }
}

@Singleton
class TruckScaleBuilder @Inject()(factory: TruckScale.BuildFactory)(implicit system: ActorSystem) {
  val logger: Logger = Logger(this.getClass)
  logger.info("Загружен TruckScaleBuilder")

  def createActor(): ActorRef = {
    val ref: ActorRef = TruckScale.createActor(factory(),java.util.UUID.randomUUID.toString )
    ref
  }

}
