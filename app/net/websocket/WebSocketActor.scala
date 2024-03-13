package net.websocket

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import com.google.inject.assistedinject.Assisted
import models.connection.WebSocketConection

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import services.storage.GlobalStorage

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object WebSocketActor {
  private val logger = org.slf4j.LoggerFactory.getLogger(classOf[WebSocketActor])

  trait Factory {
    def apply(out: ActorRef, id: String, conn: WebSocketConection): Actor
  }

  def createActorProps(create: => Actor, name: String, props: Props => Props = identity): Props = {
    props(Props(create))
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }




}


class WebSocketActor @Inject()(@Assisted out: ActorRef, @Assisted id: String, @Assisted connection: WebSocketConection)
extends Actor {

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 1000, withinTimeRange = 1 minute) {
    case _: ArithmeticException => Restart
    case _: NullPointerException => Restart
    case _: IllegalArgumentException => Stop
    case _: NoSuchElementException => Restart
    case t =>
      super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)

  }

  val logger: Logger = Logger(this.getClass)

  override def receive: Receive = {


    case msg: String =>

      val jsonTry: Try[JsValue] = Try(Json.parse(msg))
      jsonTry match {
        case Failure(exception) => out ! s"{ \"type\":\"error\",  \"errorMessage\":\"${exception.getMessage}\" }"
        case Success(json) =>
          val optType: Option[String] = (json \ "type").asOpt[String]

          optType match {
            case Some(type_) =>
              type_ match {
                case "echo" => out ! msg
                case _ =>
              }
            case None =>
          }

      }
  }

  override def preStart(): Unit = {
    logger.info(s"Pre Actor Start for connection $id")
  }

  override def postStop(): Unit  = {
    logger.info(s"Post Actor Stop - removed connection $id")
    GlobalStorage.removeConnection(id)
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    logger.info(s"Pre Restart $id")
  }

  override def postRestart(reason: Throwable): Unit =  {
    logger.info(s"Post Restart $id")
  }
}
