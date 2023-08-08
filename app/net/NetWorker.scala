package net

import akka.util.ByteString
import executioncontexts.CustomBlockingExecutionContext
import play.api.Logger
import services.start.InterfaceStart

import java.net.InetSocketAddress
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NetWorker @Inject()(start: InterfaceStart)(implicit excontext: CustomBlockingExecutionContext){
  val logger: Logger = Logger(this.getClass)
  logger.info("Создан синглтон Net Worker")

  start.helloFromService("NetWorker")

  def udpWork(data: ByteString, remote: InetSocketAddress): Unit = {
    Future {
      val msg = data.decodeString("utf-8").replaceAll("\n", " ")
      logger.info(s"${remote.getHostString}:${remote.getPort} прислал: $msg")
      start.udpDebugCommand(msg)
    }
  }
}
