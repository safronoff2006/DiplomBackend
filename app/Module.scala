import akka.actor.ActorRef
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.ConfigFactory
import executioncontexts.CustomBlockingExecutionContext
import models.configs.ProtocolsConf
import net.{TcpServer, TcpServerBuilder}
import play.api.Logger
import play.api.inject.Injector
import play.api.libs.concurrent.AkkaGuiceSupport
import services.businesslogic.channelparsers.Parser.PatternInfo
import services.businesslogic.channelparsers.{Parser, ParserAutoProtocol, ParserRailProtocol}
import services.businesslogic.dispatchers.{RailWeighbridge, RailWeighbridgeBuilder, TruckScale, TruckScaleBuilder}
import services.businesslogic.managers.PhisicalObjectsManager
import services.businesslogic.statemachines.{AutoStateMachine, RailStateMachine, StateMachine}
import services.start.{ApplicationStartDebug, InterfaceStart}
import services.storage.TcpStorage

import javax.inject.Named



class Module  extends AbstractModule  with AkkaGuiceSupport {
  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен модуль IoC контейнера")

  override def configure(): Unit = {
    logger.info("Выполняется конфигурация модуля Guice")

    bind(classOf[CustomBlockingExecutionContext]).asEagerSingleton()


    if (ConfigFactory.load.hasPath("tcp-servers.host-ip")) {
      val hostip = ConfigFactory.load.getString("tcp-servers.host-ip")
      bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance(hostip)
    } else  bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance("0.0.0.0")

    bind(classOf[String]).annotatedWith(Names.named("AutoMainPattern")).toInstance(
      if (ConfigFactory.load.hasPath("protocols.AutoMain")) {
        ProtocolsConf.getProtocolByName(ConfigFactory.load.getString("protocols.AutoMain"))
      } else ""
    )

    bind(classOf[String]).annotatedWith(Names.named("AutoMainPatternName")).toInstance(
      if (ConfigFactory.load.hasPath("protocols.AutoMain")) {
        ConfigFactory.load.getString("protocols.AutoMain")
      } else ""
    )


    bind(classOf[String]).annotatedWith(Names.named("RailsMainPattern")).toInstance(
      if (ConfigFactory.load.hasPath("protocols.RailsMain")) {
        ProtocolsConf.getProtocolByName(ConfigFactory.load.getString("protocols.RailsMain"))
      } else ""
    )

    bind(classOf[String]).annotatedWith(Names.named("RailsMainPatternName")).toInstance(
      if (ConfigFactory.load.hasPath("protocols.RailsMain")) {
        ConfigFactory.load.getString("protocols.RailsMain")
      } else ""
    )



    bind(classOf[Parser]).annotatedWith(Names.named("AutoParser")).to(classOf[ParserAutoProtocol])

    bind(classOf[Parser]).annotatedWith(Names.named("RailParser")).to(classOf[ParserRailProtocol])

    bind(classOf[StateMachine]).annotatedWith(Names.named("AutoStateMachine")).to(classOf[AutoStateMachine])
    bind(classOf[StateMachine]).annotatedWith(Names.named("RailStateMachine")).to(classOf[RailStateMachine])

    bindActorFactory[TcpServer, TcpServer.BuildFactory]

    bindActorFactory[TruckScale,TruckScale.BuildFactory]
    bindActorFactory[RailWeighbridge,RailWeighbridge.BuildFactory]

    bind(classOf[TcpServerBuilder]).asEagerSingleton()
    bind(classOf[TruckScaleBuilder]).asEagerSingleton()
    bind(classOf[RailWeighbridgeBuilder]).asEagerSingleton()
    bind(classOf[PhisicalObjectsManager]).asEagerSingleton()

    bind(classOf[TcpStorage]).asEagerSingleton()
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

  @Provides
  @Named("AutoMainPatternInfo")
  def getAutoPatternInfo(@Named("AutoMainPatternName") name:String,
                         @Named("AutoMainPattern") pattern: String): PatternInfo = (name, pattern)


  @Provides
  @Named("RailsPatternInfo")
  def getRailPatternInfo(@Named("RailsMainPatternName") name: String,
                         @Named("RailsMainPattern") pattern: String): PatternInfo = (name, pattern)




}



