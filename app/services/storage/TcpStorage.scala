package services.storage

import akka.actor.ActorRef
import models.configs.{Server, TcpConf}
import net.tcp.TcpServerBuilder
import play.api.{Configuration, Logger}

import javax.inject.{Inject, Singleton}

@Singleton
class TcpStorage @Inject()(config: Configuration, tcpBuilder: TcpServerBuilder) {
  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен TcpStorage")

  private val tcpConfiguration:   Option[TcpConf] =  if (config.has("tcp-servers")) {
    val tcpconf: TcpConf = config.get[TcpConf]("tcp-servers")
    logger.info(s"Конфигурация TCP серверов:   $tcpconf")
    Some(tcpconf)
  } else None

  private val tcpServers: Option[Seq[(String,ActorRef)]] = tcpConfiguration match {
    case None =>
      logger.warn("Не считана конфигурация TCP серверов")
      None
    case Some(tcpconf) =>
      val servers: Seq[(String,ActorRef)] = tcpconf.servers.map {
        confServer =>
          val serv = tcpBuilder.openServer(confServer.port, confServer.id, confServer.phisicalObject, confServer.channelName)
          confServer.id -> serv
      }
      Some(servers)
  }

  def getTcpConfiguration: Option[TcpConf] = tcpConfiguration
  def getTcpServers: Option[Seq[(String,ActorRef)]] = tcpServers

  def getHostIp: Option[String] = tcpConfiguration match {
    case None => None
    case Some(tcpconf) => Some(tcpconf.hostip)
  }

  def getServerConfigById(id: String):Option[Server] = {
    tcpConfiguration match {
      case None => None
      case Some(tcpconf) => tcpconf.servers.find(_.id == id)
    }
  }

  def getServerById(id:String): Option[ActorRef]= tcpServers match {
    case None => None
    case Some(servers) => servers.find(_._1 == id).map(_._2)
  }

}
