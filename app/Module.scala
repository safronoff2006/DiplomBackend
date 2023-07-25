import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.ConfigFactory
import net.{TcpServer, TcpServerBuilder}
import play.api.Logger
import play.api.libs.concurrent.AkkaGuiceSupport
import services.start.{ApplicationStartDebug, InterfaceStart}



class Module  extends AbstractModule  with AkkaGuiceSupport {
  private val logger: Logger = Logger(this.getClass)
  logger.info("DEBUG Module")

  override def configure(): Unit = {
    logger.info("DEBUG Module configure")

    if (ConfigFactory.load.hasPath("tcp-servers.host-ip")) {
      val hostip = ConfigFactory.load.getString("tcp-servers.host-ip")
      bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance(hostip)
    } else  bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance("0.0.0.0")

    bindActorFactory[TcpServer, TcpServer.BuildFactory]
    bind(classOf[TcpServerBuilder]).asEagerSingleton()
    bind(classOf[InterfaceStart]).to(classOf[ApplicationStartDebug]).asEagerSingleton()
  }


}



