package services.businesslogic.dispatchers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import services.businesslogic.dispatchers.PhisicalObject._

object RailWeighbridge {
  trait BuildFactory {
    def apply(): Actor
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }
}

class RailWeighbridge extends PhisicalObject with Actor {

  override def receive: Receive = {
    case NameEvent(n: String) =>
      setName(n)
      log.info(s"Actor named $name")

    case PrintNameEvent =>    log.info(s"Actor name $name")
    case obj:TcpMessageEvent => log.info(obj.toString)

    case _ =>
  }
}

@Singleton
class RailWeighbridgeBuilder @Inject()(factory: RailWeighbridge.BuildFactory)(implicit system: ActorSystem) {
  val logger: Logger = Logger(this.getClass)
  logger.info("Load RailWeighbridgeBuilder")

  def createActor(): ActorRef = {
    val ref: ActorRef = RailWeighbridge.createActor(factory(), java.util.UUID.randomUUID.toString )
    ref
  }
}

