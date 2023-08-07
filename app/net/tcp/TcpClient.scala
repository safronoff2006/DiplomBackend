package net.tcp

import akka.actor.{Actor, Props}
import akka.io.Tcp.Connected
import akka.io.{IO, Tcp}
import akka.util.ByteString

import java.net.InetSocketAddress


object TcpClient {
  def props(remote: InetSocketAddress, replies: TcpClientOwner): Props = Props(new TcpClient(remote, replies))
}

trait TcpClientOwner {
  def connected(c:Connected): Unit
  def message(s:String):Unit
  def data(d:ByteString, s: String):Unit
}


class TcpClient (remote: InetSocketAddress, listener: TcpClientOwner) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)
  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      listener.message(s"${remote.getAddress}:${remote.getPort}  connect failed")
      context.stop(self)

    case c@Connected(remote, local) =>
      listener.connected(c)
      val connection = sender()
      connection ! Register(self)
      context.become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          listener.message(s"${remote.getAddress}:${remote.getPort} write failed")
        case Received(data) =>
          listener.data(data, s"${remote.getAddress}:${remote.getPort}")
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener.message(s"${remote.getAddress}:${remote.getPort} connection closed")
          context.stop(self)
      }
  }

}








