package services.businesslogic.dispatchers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import play.api.Logger
import services.businesslogic.channelparsers.Parser

import javax.inject.{Inject, Named, Singleton}
import services.businesslogic.dispatchers.PhisicalObject._

object RailWeighbridge {
  trait BuildFactory {
    def apply(): Actor
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }
}

class RailWeighbridge @Inject() (@Named("RailParser") parser: Parser) extends PhisicalObject(parser: Parser) with Actor {
  log.info("Создан актор RailWeighbridge")
  parser.setDispatcher(self)

  override def receive: Receive = {
    case NameEvent(n: String) =>
      setName(n)
      log.info(s"Актор именован: $name")

    case PrintNameEvent(prefix) =>    log.info(s"$prefix назначен диспетчер физических объектов $name")
    case obj:TcpMessageEvent =>
      log.info(obj.toString)
      parser.sendToParser(obj.message)

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

