package net.udp

import akka.actor.Actor
import akka.event.{Logging, LoggingAdapter}
import akka.io.{IO, Udp}
import net.NetWorker


import java.net.InetSocketAddress

class UdpServer(host: String = "", port: Int = 0, worker: NetWorker) extends Actor {
  import context.system
  protected val logger: LoggingAdapter = Logging(context.system, this)

  override def preStart(): Unit = {
    logger.info(s"UDP Сервер стартует на $host:$port")
    if (host.isBlank) IO(Udp) ! Udp.Bind(self, new InetSocketAddress(port))
    else IO(Udp) ! Udp.Bind(self, new InetSocketAddress(host, port))
  }

  override def receive: Receive = {
    case Udp.Bound(local) =>
      logger.info(s"UDP Сервер слушает на ${local.getHostString}:${local.getPort}")

    case Udp.Received(data, remote) =>
      worker.udpWork(data, remote)
      sender() ! Udp.Send(data, remote) // эхо
    case Udp.Unbind =>
      sender() ! Udp.Unbind
    case Udp.Unbound =>
      context.stop(self)
  }


  override def postStop(): Unit = {
    logger.info(s"UDP Сервер останавливается")
  }
}
