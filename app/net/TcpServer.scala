package  net


import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.io.{IO, Tcp}
import akka.util.ByteString

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

sealed case class Message(message: String)
case object ClearConnection


class TcpServer(address: String, port: Int) extends Actor {

  import akka.io.Tcp._
  import context.system

  private val log = Logging(context.system, this)

  private val connection: AtomicReference[Option[ActorRef]] = new AtomicReference[Option[ActorRef]](None)

  IO(Tcp) ! Bind(self, new InetSocketAddress(address, port))

  def receive: Receive = {

    case b @ Bound(_) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context.stop(self)

    case c @ Connected(remote, local) =>


      val connection: ActorRef = sender()
      val hndProps: Props = Props( new SimplisticHandler( self, connection, local))
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

class SimplisticHandler(manager:ActorRef, connection: ActorRef, local:InetSocketAddress) extends Actor {

  import Tcp._

  private val log = Logging(context.system, this)
  context.watch(connection)

  override def postStop(): Unit = {
    log.info(s"Handler Stop for connection $connection")
  }

  def receive: Receive = {
    case Received(data) =>
      val str = data.utf8String.trim + "!!!\n"
      sender() ! Write(ByteString(str))

    case PeerClosed =>
      log.info(s"Connection closed $connection")
      manager ! ClearConnection
      context.stop(self)

  }


}




object TcpServer {

  def openServer(address: String, port: Int)(implicit system: ActorSystem): ActorRef = {
    val serverProps: Props = Props( new TcpServer(address, port))
    val server = system.actorOf(serverProps)
    server
  }


}