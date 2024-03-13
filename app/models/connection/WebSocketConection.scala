package models.connection

import akka.actor.ActorRef
import play.api.mvc.RequestHeader

case class WebSocketConection(request: RequestHeader, out:ActorRef)
