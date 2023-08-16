package services.businesslogic.dispatchers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import services.businesslogic.channelparsers.Parser
import services.businesslogic.channelparsers.Parser.PatternInfo

import javax.inject.{Inject, Named, Singleton}
import services.businesslogic.dispatchers.PhisicalObject._
import services.businesslogic.statemachines.StateMachine

object RailWeighbridge {
  trait BuildFactory {
    def apply(): Actor
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }
}

class RailWeighbridge @Inject() (@Named("RailParser") parser: Parser,
                                 @Named("RailStateMachine") stateMachine: StateMachine,
                                 @Named("RailsPatternInfo") mainProtocolPattern: PatternInfo)

  extends PhisicalObject(parser: Parser, stateMachine: StateMachine, mainProtocolPattern: PatternInfo) with Actor {

  log.info("Создан диспетчер RailWeighbridge")
  parser.setDispatcher(self)
  parser.setPattern(mainProtocolPattern)

  override def receive: Receive = {
    case NameEvent(n: String) =>
      setName(n)
      log.info(s"Диспетчер именован: $name")
      stateMachine.name = n

    case PrintNameEvent(prefix) =>    log.info(s"$prefix назначен диспетчер физических объектов $name")
    case obj:TcpMessageEvent => parser.sendToParser(obj.message)
    case obj:RailWeight => stateMachine.protocolMessage(obj)
    case _ =>
  }
}

@Singleton
class RailWeighbridgeBuilder @Inject()(factory: RailWeighbridge.BuildFactory)(implicit system: ActorSystem) {
  val logger: Logger = Logger(this.getClass)
  logger.info("Загружен RailWeighbridgeBuilder")

  def createActor(): ActorRef = {
    val ref: ActorRef = RailWeighbridge.createActor(factory(), java.util.UUID.randomUUID.toString )
    ref
  }
}

