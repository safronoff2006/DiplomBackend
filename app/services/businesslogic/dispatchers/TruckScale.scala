package services.businesslogic.dispatchers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import services.businesslogic.dispatchers.PhisicalObject._

object TruckScale {
  trait BuildFactory {
    def apply(): Actor
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }
}

class TruckScale extends PhisicalObject with Actor  {
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
class TruckScaleBuilder @Inject()(factory: TruckScale.BuildFactory)(implicit system: ActorSystem) {
  val logger: Logger = Logger(this.getClass)
  logger.info("Load TruckScaleBuilder")

  def createActor(): ActorRef = {
    val ref: ActorRef = TruckScale.createActor(factory(),java.util.UUID.randomUUID.toString )
    ref
  }

}
