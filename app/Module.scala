import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.ConfigFactory
import executioncontexts.CustomBlockingExecutionContext
import models.configs.ProtocolsConf
import net.NetWorker
import net.tcp.{TcpServer, TcpServerBuilder}
import net.udp.UdpServerManager
import play.api.Logger
import play.api.inject.Injector
import play.api.libs.concurrent.AkkaGuiceSupport
import services.businesslogic.channelparsers.Parser.PatternInfo
import services.businesslogic.channelparsers.{Parser, ParserAutoProtocol, ParserRailProtocol}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.dispatchers.typed.{RailWeighbridgeTyped, RailWeighbridgeWrapper, TruckScaleTyped, TruckScaleWrapper}
import services.businesslogic.managers.PhisicalObjectsManager
import services.businesslogic.statemachines.{AutoStateMachine, RailStateMachine, StateMachine}
import services.start.{ApplicationStartDebug, InterfaceStart}
import services.storage.GlobalStorage._
import services.storage.{GlobalStorage, StateMachinesStorage, TcpStorage}

import javax.inject.Named
import scala.annotation.tailrec


class Module  extends AbstractModule  with AkkaGuiceSupport {



  object MainBehavior {
    def apply(): Behavior[MainBehaviorCommand] =
      Behaviors.setup { context =>
        context.log.info("Creation of the main Behavior")

        Behaviors.receiveMessage { msg =>
          msg match {
            case CreateTruckScaleDispatcher(parser, stateMachine, mainProtocolPattern, id) =>
              val actorDispatcherBehavior: Behavior[PhisicalObjectEvent] = Behaviors.setup[PhisicalObjectEvent] { ctx =>
                new TruckScaleTyped(ctx, parser, stateMachine, mainProtocolPattern)
              }

             val ref: ActorRef[PhisicalObjectEvent] =  context.spawn(actorDispatcherBehavior, id )
              context.log.info(s"Create actorDispatcher $id")
              GlobalStorage.setRef(id,ref)
              Behaviors.same

            case   CreateRailWeighbridgeDispatcher(parser, stateMachine, mainProtocolPattern, id) =>
              val actorDispatcherBehavior: Behavior[PhisicalObjectEvent] = Behaviors.setup[PhisicalObjectEvent] { ctx =>
                new RailWeighbridgeTyped(ctx, parser, stateMachine, mainProtocolPattern)
              }

             val ref: ActorRef[PhisicalObjectEvent] = context.spawn(actorDispatcherBehavior, id)
              context.log.info(s"Create actorDispatcher $id")
              GlobalStorage.setRef(id, ref)
              Behaviors.same

            case _ => Behaviors.same
          }
        }
      }
  }

  private def createMainContext = {
    implicit val sys: akka.actor.typed.ActorSystem[MainBehaviorCommand] = akka.actor.typed.ActorSystem(MainBehavior(), "main")
    sys
  }


  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен модуль IoC контейнера")
  val sys: ActorSystem[MainBehaviorCommand] =  createMainContext
  GlobalStorage.setSys(sys)


  override def configure(): Unit = {
    logger.info("Выполняется конфигурация модуля Guice")


    //привязка таймаута обработки карты
    bind(classOf[Long]).annotatedWith(Names.named("CardTimeout")).toInstance(
      if (ConfigFactory.load.hasPath("timeoutCardResponce"))  ConfigFactory.load.getLong("timeoutCardResponce")
      else 5000
    )

    //привязка флага конвертации кода EmMarine из Hex в Text
    if (ConfigFactory.load.hasPath("convert_HexEmMarine_to_TextEmMarine")) {
      val convert = ConfigFactory.load.getBoolean("convert_HexEmMarine_to_TextEmMarine")
      bind(classOf[Boolean]).annotatedWith(Names.named("ConvertEmMarine")).toInstance(convert)
    } else bind(classOf[Boolean]).annotatedWith(Names.named("ConvertEmMarine")).toInstance(false)


    //привязка флага проверки CRC
    if (ConfigFactory.load.hasPath("useCRC")) {
      val useCRC = ConfigFactory.load.getBoolean("useCRC")
      bind(classOf[Boolean]).annotatedWith(Names.named("UseCRC")).toInstance(useCRC)
    } else bind(classOf[Boolean]).annotatedWith(Names.named("UseCRC")).toInstance(false)

    //привязка пользовательского контекста исполнения для блокирующих и длительных операций
    bind(classOf[CustomBlockingExecutionContext]).asEagerSingleton()

    //привязка host-ip из конфигурации TCP серверов
    if (ConfigFactory.load.hasPath("tcp-servers.host-ip")) {
      val hostip = ConfigFactory.load.getString("tcp-servers.host-ip")
      bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance(hostip)
    } else  bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance("0.0.0.0")

    //приявязка паттернов
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

    bind(classOf[String]).annotatedWith(Names.named("CardPatternName")).toInstance(
      if (ConfigFactory.load.hasPath("card")) {
        ConfigFactory.load.getString("card")
      } else ""
    )

    bind(classOf[String]).annotatedWith(Names.named("CardPattern")).toInstance(
      if (ConfigFactory.load.hasPath("card")) {
        ProtocolsConf.getProtocolByName(ConfigFactory.load.getString("card"))
      } else ""
    )


    //привязка сервисов
    bind(classOf[InterfaceStart]).to(classOf[ApplicationStartDebug]).asEagerSingleton()
    bind(classOf[GlobalStorage]).asEagerSingleton()
    bind(classOf[StateMachinesStorage]).asEagerSingleton()

    bind(classOf[Parser]).annotatedWith(Names.named("AutoParser")).to(classOf[ParserAutoProtocol])
    bind(classOf[Parser]).annotatedWith(Names.named("RailParser")).to(classOf[ParserRailProtocol])

    bind(classOf[StateMachine]).annotatedWith(Names.named("AutoStateMachine")).to(classOf[AutoStateMachine])
    bind(classOf[StateMachine]).annotatedWith(Names.named("RailStateMachine")).to(classOf[RailStateMachine])

    bindActorFactory[TcpServer, TcpServer.BuildFactory]




    //bindActorFactory[TruckScale,TruckScale.BuildFactory] //убрать потоом
    //bindActorFactory[RailWeighbridge,RailWeighbridge.BuildFactory]

    bind(classOf[TcpServerBuilder]).asEagerSingleton()

    //bind(classOf[TruckScaleBuilder]).asEagerSingleton() //убрать потом
    //bind(classOf[RailWeighbridgeBuilder]).asEagerSingleton()

    bind(classOf[PhisicalObjectsManager]).asEagerSingleton()

    bind(classOf[TcpStorage]).asEagerSingleton()

    bind(classOf[NetWorker]).asEagerSingleton()
    bind(classOf[UdpServerManager]).asEagerSingleton()




  }

  object GetRefWhenExist {
    @tailrec
    def getRef(id: String): ActorRef[PhisicalObjectEvent] = {
      val optref: Option[ActorRef[PhisicalObjectEvent]] = GlobalStorage.getRef(id)
      optref match {
        case Some(ref) => ref
        case None => getRef(id)
      }
    }
  }




  //провайдеры акторов диспетчеров для ЖД и автомобильных весов
  @Provides
  @Named("RailWeighbridge")
  def getRailWeighbridgeActor(injector: Injector) = {
    val builder = injector.instanceOf[RailWeighbridgeWrapper]
    val id = builder.create()
    GetRefWhenExist.getRef(id)
  }


  @Provides
  @Named("TruckScale")
  def getTruckScaleActor(injector: Injector): akka.actor.typed.ActorRef[PhisicalObjectEvent] = {
    val builder = injector.instanceOf[TruckScaleWrapper]
    val id = builder.create()
    GetRefWhenExist.getRef(id)

  }


  //провайдеры кортежей паттернов для парсеров протоколов
  @Provides
  @Named("AutoMainPatternInfo")
  def getAutoPatternInfo(@Named("AutoMainPatternName") nameMainPattern:String,
                         @Named("AutoMainPattern") mainPattern: String,
                         @Named("CardPatternName") nameCardPattern: String,
                         @Named("CardPattern") cardPattern: String): PatternInfo =
    (nameMainPattern, mainPattern, nameCardPattern, cardPattern)


  @Provides
  @Named("RailsPatternInfo")
  def getRailPatternInfo(@Named("RailsMainPatternName") nameMainPattern: String,
                         @Named("RailsMainPattern") mainPattern: String): PatternInfo =
    (nameMainPattern, mainPattern, "", "")




}



