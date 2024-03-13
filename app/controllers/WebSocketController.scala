package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.Logger
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}

import javax.inject.{Inject,Singleton}
import models.connection.WebSocketConection
import net.websocket.WebSocketActor
import services.storage.GlobalStorage

import java.util.concurrent.ConcurrentHashMap

object WebSocketController {
  type MapWsType = ConcurrentHashMap[String, WebSocketConection]
}


@Singleton
class WebSocketController @Inject()(cc: ControllerComponents, childFactory: WebSocketActor.Factory)(implicit system: ActorSystem, mat: Materializer)
extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Create WebSocketController")
  private def uuid: String = java.util.UUID.randomUUID.toString

  def socket: WebSocket = WebSocket.accept[String,String]{ request =>
    ActorFlow.actorRef { out => {
      val id = uuid
      logger.info(s"Accept connection $id")
      val connection = WebSocketConection(request, out)
      GlobalStorage.setConnection(id, connection)

      WebSocketActor.createActorProps(childFactory(out, id, connection), "WS Actor Unit")


    }

    }
  }

}
