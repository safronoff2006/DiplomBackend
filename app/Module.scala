import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, MailboxSelector}
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.ConfigFactory
import executioncontexts.CustomBlockingExecutionContext
import models.configs.ProtocolsConf
import net.NetWorker
import net.tcp.{TcpServer, TcpServerBuilder}
import net.udp.UdpServerManager
import net.websocket.WebSocketActor
import play.api.Logger
import play.api.inject.Injector
import play.api.libs.concurrent.AkkaGuiceSupport
import services.businesslogic.channelparsers.typed.ParserTyped.PatternInfo
import services.businesslogic.channelparsers.typed.ParserTyped.ParserCommand
import services.businesslogic.channelparsers.typed._
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.dispatchers.typed.{RailWeighbridgeTyped, RailWeighbridgeWrapper, TruckScaleTyped, TruckScaleWrapper}
import services.businesslogic.managers.PhisicalObjectsManager
import services.businesslogic.statemachines.typed.StateMachineTyped.StateMachineCommand
import services.businesslogic.statemachines.typed.{AutoStateMachineTyped, AutoStateMachineWraper, RailStateMachineTyped, RailStateMachineWraper, StateMachineWraper}
import services.db.DbLayer._
import services.start.{ApplicationStartDebug, InterfaceStart}
import services.storage.GlobalStorage._
import services.storage.{GlobalStorage, StateMachinesStorage, TcpStorage}

import javax.inject.Named
import scala.annotation.tailrec

class Module extends AbstractModule with AkkaGuiceSupport {

  private object MainBehavior {
    def apply(): Behavior[MainBehaviorCommand] =
      Behaviors.setup { context =>
        context.log.info("Creation of the main Behavior")

        Behaviors.receiveMessage {
          case CreateTruckScaleDispatcher(parser, stateMachine, mainProtocolPattern, id) =>
            val actorDispatcherBehavior: Behavior[PhisicalObjectEvent] = Behaviors.setup[PhisicalObjectEvent] { ctx =>
              new TruckScaleTyped(ctx, parser, stateMachine, mainProtocolPattern)
            }

            val ref: ActorRef[PhisicalObjectEvent] = context.spawn(actorDispatcherBehavior, id)
            context.log.info(s"Create actorDispatcher $id")
            GlobalStorage.setRefPOE(id, ref)
            Behaviors.same

          case CreateRailWeighbridgeDispatcher(parser, stateMachine, mainProtocolPattern, id) =>
            val actorDispatcherBehavior: Behavior[PhisicalObjectEvent] = Behaviors.setup[PhisicalObjectEvent] { ctx =>
              new RailWeighbridgeTyped(ctx, parser, stateMachine, mainProtocolPattern)
            }

            val ref: ActorRef[PhisicalObjectEvent] = context.spawn(actorDispatcherBehavior, id)
            context.log.info(s"Create actorDispatcher $id")
            GlobalStorage.setRefPOE(id, ref)
            Behaviors.same

          case CreateAutoProtocolParser(id) =>
            val actorParserBehavior: Behavior[ParserCommand] = Behaviors.setup[ParserCommand] { ctx =>
              new ParserAutoProtocolTyped(ctx)
            }

            val ref = context.spawn(actorParserBehavior, id)
            context.log.info(s"Create actorProtocol $id")
            GlobalStorage.setRefParser(id, ref)
            Behaviors.same

          case CreateRailProtocolParser(id) =>
            val actorParserBehavior: Behavior[ParserCommand] = Behaviors.setup[ParserCommand] { ctx =>
              new ParserRailProtocolTyped(ctx)
            }

            val ref = context.spawn(actorParserBehavior, id)
            context.log.info(s"Create actorProtocol $id")
            GlobalStorage.setRefParser(id, ref)
            Behaviors.same

          case CreateAutoStateMachine(nameCardPattern, stateStorage, convertEmMarine, cardTimeout, id) =>
            val actorStateMachineBehavior: Behavior[StateMachineCommand] = Behaviors.setup[StateMachineCommand] { ctx =>
              new AutoStateMachineTyped(ctx, nameCardPattern, stateStorage, convertEmMarine, cardTimeout)
            }

            val props = MailboxSelector.fromConfig("mailboxes.state-machine-mailbox")
            val ref = context.spawn(actorStateMachineBehavior, id, props)

            context.log.info(s"Create actorStateMachine $id")
            GlobalStorage.setRefSM(id, ref)
            Behaviors.same

          case CreateRailStateMachine(stateStorage, id) =>
            val actorStateMachineBehavior: Behavior[StateMachineCommand] = Behaviors.setup[StateMachineCommand] { ctx =>
              new RailStateMachineTyped(ctx, stateStorage)
            }
            val props = MailboxSelector.fromConfig("mailboxes.state-machine-mailbox")
            val ref = context.spawn(actorStateMachineBehavior, id, props)
            context.log.info(s"Create actorStateMachine $id")
            GlobalStorage.setRefSM(id, ref)
            Behaviors.same

          case _ => Behaviors.same
        }
      }
  }

  private def createMainContext = {
    implicit val sys: akka.actor.typed.ActorSystem[MainBehaviorCommand] = akka.actor.typed.ActorSystem(MainBehavior(), "main")
    sys
  }


  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен модуль IoC контейнера")
  val sys: ActorSystem[MainBehaviorCommand] = createMainContext
  GlobalStorage.setSys(sys)


  override def configure(): Unit = {
    logger.info("Выполняется конфигурация модуля Guice")

    //привязка конфигурации параметров операций вставки
    val notExistConf = InsertConf(InsertInnerConf(50, 10, 5), InsertInnerConf(50, 10, 5), InsertInnerConf(50, 10, 5))

    val insertConfObject: InsertConf = if (ConfigFactory.load.hasPath("insertConf")) {
      val confTest = ConfigFactory.load.getConfig("insertConf.test")
      val confState = ConfigFactory.load.getConfig("insertConf.state")
      val confCard = ConfigFactory.load.getConfig("insertConf.card")

      val innerTest = InsertInnerConf(confTest.getInt("listMaxSize"), confTest.getInt("groupSize"), confTest.getInt("parallelism"))
      val innerState = InsertInnerConf(confState.getInt("listMaxSize"), confState.getInt("groupSize"), confState.getInt("parallelism"))
      val innerCard = InsertInnerConf(confCard.getInt("listMaxSize"), confCard.getInt("groupSize"), confCard.getInt("parallelism"))

      InsertConf(innerTest, innerState, innerCard)
    } else notExistConf



    bind(classOf[InsertConf]).toInstance(insertConfObject)

    //привязка конфигурации web-протокола


    if (ConfigFactory.load.hasPath("webProtocols")) {
      val protocol = ConfigFactory.load.getConfig("webProtocols.use")
      val name = protocol.getString("name")
      val endPoint = protocol.getString("endPoint")

      val instanceWebProtokol = WebProtokol(name, endPoint)
      bind(classOf[WebProtokol]).toInstance(instanceWebProtokol)
    } else {
      bind(classOf[WebProtokol]).toInstance(WebProtokol("", ""))
    }


    //привязка таймаута обработки карты
    bind(classOf[Long]).annotatedWith(Names.named("CardTimeout")).toInstance(
      if (ConfigFactory.load.hasPath("timeoutCardResponce")) ConfigFactory.load.getLong("timeoutCardResponce")
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
    } else bind(classOf[String]).annotatedWith(Names.named("HostIp")).toInstance("0.0.0.0")

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

    bind(classOf[ParserWraper]).annotatedWith(Names.named("AutoParserW")).to(classOf[ParserAutoProtocolWraper])
    bind(classOf[ParserWraper]).annotatedWith(Names.named("RailParserW")).to(classOf[ParserRailProtokolWraper])


    bind(classOf[StateMachineWraper]).annotatedWith(Names.named("AutoStateMachineW")).to(classOf[AutoStateMachineWraper])
    bind(classOf[StateMachineWraper]).annotatedWith(Names.named("RailStateMachineW")).to(classOf[RailStateMachineWraper])

    bindActorFactory[TcpServer, TcpServer.BuildFactory]

    bind(classOf[TcpServerBuilder]).asEagerSingleton()

    bind(classOf[PhisicalObjectsManager]).asEagerSingleton()

    bind(classOf[TcpStorage]).asEagerSingleton()

    bind(classOf[NetWorker]).asEagerSingleton()
    bind(classOf[UdpServerManager]).asEagerSingleton()

    bindActorFactory[WebSocketActor, WebSocketActor.Factory]


  }

  private object GetRefWhenExist {
    @tailrec
    def getRefPOE(id: String): ActorRef[PhisicalObjectEvent] = {
      val optref: Option[ActorRef[PhisicalObjectEvent]] = GlobalStorage.getRefPOE(id)
      optref match {
        case Some(ref) => ref
        case None => getRefPOE(id)
      }
    }

    @tailrec
    def getRefParser(id: String): ActorRef[ParserCommand] = {
      val optref = GlobalStorage.getRefParser(id)
      optref match {
        case Some(ref) => ref
        case None => getRefParser(id)
      }
    }

    @tailrec
    def getRefST(id: String): ActorRef[StateMachineCommand] = {
      val optref = GlobalStorage.getRefSM(id)
      optref match {
        case Some(ref) => ref
        case None => getRefST(id)
      }
    }
  }


  //провайдеры акторов диспетчеров для ЖД и автомобильных весов
  @Provides
  @Named("RailWeighbridge")
  def getRailWeighbridgeActor(injector: Injector): ActorRef[PhisicalObjectEvent] = {
    val builder = injector.instanceOf[RailWeighbridgeWrapper]
    val id = builder.create()
    GetRefWhenExist.getRefPOE(id)
  }


  @Provides
  @Named("TruckScale")
  def getTruckScaleActor(injector: Injector): ActorRef[PhisicalObjectEvent] = {
    val builder = injector.instanceOf[TruckScaleWrapper]
    val id = builder.create()
    GetRefWhenExist.getRefPOE(id)

  }

  //провайдеры акторов парсеров

  @Provides
  @Named("AutoParserA")
  def getAutoProtocolParserActor(@Named("AutoParserW") wraper: ParserWraper): ActorRef[ParserCommand] = {
    val id = wraper.create()
    GetRefWhenExist.getRefParser(id)
  }

  @Provides
  @Named("RailParserA")
  def getRailProtocolParserActor(@Named("RailParserW") wraper: ParserWraper): ActorRef[ParserCommand] = {
    val id = wraper.create()
    GetRefWhenExist.getRefParser(id)
  }

  //провайдеры акторов стейт-машин

  @Provides
  @Named("AutoStateMachineA")
  def getAutoStateMachineActor(@Named("AutoStateMachineW") sm: StateMachineWraper): ActorRef[StateMachineCommand] = {
    val id = sm.create()
    GetRefWhenExist.getRefST(id)
  }

  @Provides
  @Named("RailStateMachineA")
  def getRailStateMachineActor(@Named("RailStateMachineW") sm: StateMachineWraper): ActorRef[StateMachineCommand] = {
    val id = sm.create()
    GetRefWhenExist.getRefST(id)
  }

  //провайдеры кортежей паттернов для парсеров протоколов
  @Provides
  @Named("AutoMainPatternInfo")
  def getAutoPatternInfo(@Named("AutoMainPatternName") nameMainPattern: String,
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



