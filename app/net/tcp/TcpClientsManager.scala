package net.tcp

import akka.actor.{ActorRef, ActorSystem, Cancellable, Scheduler}
import akka.io.Tcp
import akka.util.ByteString
import models.configs.Server
import play.api.Logger
import services.storage.TcpStorage
import utils.AtomicOption

import java.net.InetSocketAddress
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.CollectionHasAsScala


case class ClientConf(id:String, port: Int, client: ActorRef )

@Singleton
class TcpClientsManager @Inject()(tcpStorage: TcpStorage, system: ActorSystem,
                                  @Named("HostIp") hostip: String) extends TcpClientOwner {

  val logger: Logger = Logger(this.getClass)
  logger.info("Загружен TestTcpClientsManager")

  private val mapClients: ConcurrentHashMap[String, ClientConf] = new ConcurrentHashMap()
  private val mapData: ConcurrentHashMap[String, Option[String]] = new ConcurrentHashMap()
  private val  scheduler: Scheduler = system.scheduler
  private implicit val disp: ExecutionContextExecutor = system.dispatcher

  private val optCancel: AtomicOption[Cancellable] = new AtomicOption(None)

  def stop(): Unit = {
    logger.info("Остановка TestTcpClientsManager ")
    mapClients.forEach{
      (key,value) =>
          value.client ! "close"
    }

    mapClients.clear()
    mapData.clear()
  }


  def init(): Unit = {
    logger.info("Инициализация TestTcpClientsManager ")
    stop()

    val optServersConfigs: Option[Seq[Server]] =  tcpStorage.getTcpServers match {
      case Some(servers) => Some(servers.map(_._1).flatMap(tcpStorage.getServerConfigById))
      case None =>
        logger.warn("Не обнаружены TCP серверы")
        None
    }

    if (optServersConfigs.isDefined) {
      val seqConf = optServersConfigs.get
      seqConf.foreach {
        server =>
          logger.info(s"Найден сервер с ID ${server.id}")

          val remoteSocket = new InetSocketAddress(hostip, server.port)
          val client = system.actorOf(TcpClient.props(remoteSocket, this), java.util.UUID.randomUUID.toString)
          mapClients.put(server.id, ClientConf(server.id, server.port, client))
          mapData.put(server.id, None)
      }

     mapClients
       .values()
       .asScala
       .map(_.client)
       .foreach(_ ! ConnectToServer)

    }

  }

  def setData(id: String, data: String): Unit = {

      if (mapData.containsKey(id)) {
        mapData.put(id, Some(data))
        if (mapClients.containsKey(id)) {
          val clientObject: ClientConf = mapClients.get(id)
          clientObject.client !  ByteString(data)
        }
      }
  }


  private val teskRepeat:  java.lang.Runnable = () => {
    mapData.forEach {
      (id, data) =>
        if (data.isDefined && mapClients.containsKey(id)) {
          val clientObject: ClientConf = mapClients.get(id)
          clientObject.client !  ByteString(data.get)
        }
    }
  }


  def repeat(delay: Int): Unit = optCancel.getState match {
    case None =>
      val initDelay = Duration(0, TimeUnit.MILLISECONDS)
      val repeatDelay = Duration(delay, TimeUnit.MILLISECONDS)
      val cancelable: Cancellable = scheduler.scheduleAtFixedRate(initDelay, repeatDelay)(teskRepeat)
      optCancel.setState(Some(cancelable))
    case _ =>
  }

  def norepeat(): Unit = optCancel.getState match {
    case Some(cancelable) =>
      cancelable.cancel()
      optCancel.setState(None)
    case _ =>
  }



  override def connected(c: Tcp.Connected): Unit = {
    logger.info(s"TCP клиент ${c.localAddress.getAddress}:${c.localAddress.getPort} " +
      s"соединился с сервером ${c.remoteAddress.getAddress}:${c.remoteAddress.getPort}")
  }

  override def message(s: String): Unit = logger.info(s)

  override def data(d: ByteString, s: String): Unit = logger.info(s"Данные из $s   ${d.utf8String}")
}
