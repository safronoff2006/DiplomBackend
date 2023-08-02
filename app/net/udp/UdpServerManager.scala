package net.udp

import akka.actor.{ActorRef, ActorSystem, Props}
import models.configs.Udpconf
import net.NetWorker
import play.api.{Configuration, Logger}

import javax.inject.{Inject, Singleton}

@Singleton
class UdpServerManager @Inject()(config: Configuration, system: ActorSystem, worker: NetWorker) {
  val logger: Logger = Logger(this.getClass)
  logger.info("Загружен UdpServerManager")

  val server: Option[ActorRef] = if (config.has("udp-server")) {
    val udpconf = config.get[Udpconf]("udp-server")
    if (!udpconf.host.isBlank) logger.info(s"UDP host: ${udpconf.host}")
    logger.info(s"UDP port: ${udpconf.port}")

    if (!udpconf.port.equals(0)) {
      Some(system.actorOf(Props(classOf[UdpServer], udpconf.host, udpconf.port, worker)))
    } else {
      logger.error("Не правильная конфигурация UDP Сервера")
      None
    }

  } else {
    logger.error("UDP сервер не сконфигурирован")
    None
  }

  server match {
    case Some(_) => logger.info("UDP Сервер стартовал")
    case _ =>
  }

}
