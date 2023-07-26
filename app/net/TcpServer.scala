package  net


import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.google.inject.assistedinject.Assisted
import net.TcpServer.createActor
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport
import services.businesslogic.dispatchers.PhisicalObject._
import services.businesslogic.managers.PhisicalObjectsManager

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import javax.inject.{Inject, Named, Singleton}
import scala.annotation.tailrec

sealed case class Message(message: String)
case object ClearConnection

sealed case class TcpServerParams(port: Int, id:String, phisicalObject: String, channelName: String )


class TcpServer @Inject() (@Named("HostIp") address: String,
                           manager: PhisicalObjectsManager,
                           @Assisted params: TcpServerParams) extends Actor {

  import akka.io.Tcp._
  import context.system

  private val log = Logging(context.system, this)

  log.info(s"Parameters TCP Server: $address  ${params.port}  ${params.id}  ${params.phisicalObject}  ${params.channelName}")
  private val phisicalOpt: Option[ActorRef] = manager.getPhisicalObjectByName(params.phisicalObject)

  phisicalOpt match {
    case Some(ph) => ph ! PrintNameEvent(s"TCP серверу ${params.id} на порту ${params.port} ")
    case None => log.error(s"Не найден физичяеский объект по имени ${params.phisicalObject}")
  }

  private val connection: AtomicReference[Option[ActorRef]] = new AtomicReference[Option[ActorRef]](None)

  IO(Tcp) ! Bind(self, new InetSocketAddress(address, params.port))

  def receive: Receive = {

    case b @ Bound(_) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context.stop(self)

    case c @ Connected(remote, local) =>


      val connection: ActorRef = sender()
      val hndProps: Props = Props( new SimplisticHandler( self, connection, local, phisicalOpt, params))
      val handler: ActorRef = context.actorOf(hndProps)

      connection ! Register(handler)

      setConnection(connection, local.getPort)
      log.info(s"Connection open remote ${remote.getAddress}:${remote.getPort} to local ${local.getAddress}:${local.getPort}")

      case ClearConnection => clearConnection()

      case Message(m)  => getConnection match {
        case Some(connection) => connection ! Write(ByteString(m))
        case None => log.warning(s"Connection not exist. Can not send message $m")
      }

  }

  @tailrec
  private def clearConnection(): Unit = {
    val old: Option[ActorRef] = connection.get()
    if (connection.compareAndSet(old, None)) log.info("Clear connection")
    else clearConnection()
  }

  private def getConnection = connection.get()



  @tailrec
  private def setConnection(newConnection: ActorRef, port: Int): Unit = {
    val old: Option[ActorRef] = connection.get()
    if (connection.compareAndSet(old,Some(newConnection))) log.info(s"Set connection $newConnection for port $port")
    else setConnection(newConnection, port)
  }


}

class SimplisticHandler(manager:ActorRef,
                        connection: ActorRef,
                        local:InetSocketAddress,
                        phisicalOpt: Option[ActorRef] ,
                        params: TcpServerParams) extends Actor {

  import Tcp._


  private val log = Logging(context.system, this)
  context.watch(connection)

  override def postStop(): Unit = {
    log.info(s"Handler Stop for connection $connection")
  }

  def receive: Receive = {
    case Received(data) =>
      val strEcho = data.utf8String.trim + "!!!\n"
      sender() ! Write(ByteString(strEcho))

      val strData = data.utf8String.trim

      phisicalOpt match {
        case Some(phisical) =>
          phisical ! TcpMessageEvent(params.id, params.phisicalObject, params.channelName, strData )
        case None => log.error(s"Не найден физический объект для TCP - сервера на порту ${local.getPort}")
      }

    case PeerClosed =>
      log.info(s"Connection closed $connection")
      manager ! ClearConnection
      context.stop(self)

  }


}

@Singleton
class TcpServerBuilder @Inject()(factory: TcpServer.BuildFactory)(implicit system: ActorSystem) extends InjectedActorSupport  {
  val logger: Logger = Logger(this.getClass)
  logger.info("Load TcpServerBuilder")

  def openServer(port: Int, id:String, phisicalObject: String, channelName: String ):ActorRef = {

    val params = TcpServerParams(port, id, phisicalObject, channelName )

    val server: ActorRef = createActor(factory(params), port.toString)
    server
  }

}


object TcpServer {

  trait  BuildFactory {
    def apply(params: TcpServerParams ):Actor
  }

  def createActor(create: => Actor, name: String, props: Props => Props = identity)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(props(Props(create)), name)
  }

}