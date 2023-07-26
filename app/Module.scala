import akka.actor.ActorRef
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.ConfigFactory
import net.{TcpServer, TcpServerBuilder}
import play.api.Logger
import play.api.inject.Injector
import play.api.libs.concurrent.AkkaGuiceSupport
import services.businesslogic.dispatchers.{RailWeighbridge, RailWeighbridgeBuilder, TruckScale, TruckScaleBuilder}
import services.businesslogic.managers.PhisicalObjectsManager
import services.start.{ApplicationStartDebug, InterfaceStart}

import javax.inject.Named



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

    bindActorFactory[TruckScale,TruckScale.BuildFactory]
    bindActorFactory[RailWeighbridge,RailWeighbridge.BuildFactory]

    bind(classOf[TcpServerBuilder]).asEagerSingleton()
    bind(classOf[TruckScaleBuilder]).asEagerSingleton()
    bind(classOf[RailWeighbridgeBuilder]).asEagerSingleton()
    bind(classOf[PhisicalObjectsManager]).asEagerSingleton()

    bind(classOf[InterfaceStart]).to(classOf[ApplicationStartDebug]).asEagerSingleton()
  }

  @Provides
  @Named("RailWeighbridge")
  def getRailWeighbridgeActor(injector: Injector): ActorRef = {
    val builder = injector.instanceOf[RailWeighbridgeBuilder]
    builder.createActor()
  }

  @Provides
  @Named("TruckScale")
  def getTruckScaleActor(injector: Injector): ActorRef = {
    val builder = injector.instanceOf[TruckScaleBuilder]
    builder.createActor()
  }


}



