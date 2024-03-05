package services.start

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.util.ByteString
import models.configs.Serverconf
import net.tcp._
import play.api.inject.{ApplicationLifecycle, Injector}
import play.api.libs.json.{JsValue, Json}
import play.api.{Application, Configuration, Logger, Play}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped
import services.businesslogic.managers.PhisicalObjectsManager

import java.util.Locale
import javax.inject._
import scala.concurrent.Future
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try, Using}

trait InterfaceStart {
  def udpDebugCommand(command: String): Unit
  def helloFromService(service:String):Unit
}

@Singleton
class ApplicationStartDebug @Inject()(lifecycle: ApplicationLifecycle, environment: play.api.Environment,
                                      injector: Injector, config: Configuration,tcpBuilder: TcpServerBuilder,
                                      implicit val system: ActorSystem, tcpClientsManager: TestTcpClientsManager,
                                      dispatchers: PhisicalObjectsManager)
  extends InterfaceStart with TcpClientOwner {

  val logger: Logger = Logger(this.getClass)
  logger.info("Отладочная реализация ApplicationStart")
  // хук шатдауна
  lifecycle.addStopHook { () =>
    val mess = "Остановка Oilserver"
    logger.info(mess)
    Future.successful(mess)
  }

  logger.info("Старт Oilserver")
  logger.info("ОС " +
    sys.props.get("os.name").fold("Другая": String) { name =>
      name.toLowerCase(Locale.ENGLISH) match {
        case mac if mac.contains("darwin") || mac.contains("mac") => "Mac"
        case windows if windows.contains("windows") => "Windows"
        case linux if linux.contains("linux") => "Linux"
        case _ => "Другая"
      }
    }
  )

  logger.info("Корневой путь " + environment.rootPath.getPath)

  //
  private def loadJSONFromFilename(filename: String): Try[JsValue] = Try {
    Json.parse(Using.resource(Source.fromFile(filename)) {
      _.mkString
    })
  }


  //чтение настроек из JSON файла, применять не будем, будем использовать гораздо более мощные
  //средства конфигурации Play Framework

  private val rootpath = environment.rootPath.getPath
  private val serverconfPath = rootpath + "/conf/serverconf.json"
  private val optJsServerConf = loadJSONFromFilename(serverconfPath)

  optJsServerConf match {
    case Success(i) =>
      logger.info("Конфигурация ")
      logger.info(Json.prettyPrint(i))
    case Failure(e) =>
      logger.error("Исключение при чтении или парсинге serverconf.json : " + e.toString)
      Play.stop(injector.instanceOf[Application])
  }


  //чтение основной конфигурации сервера
  if (config.has("serverconf")) {
    val serverconf: Serverconf = config.get[Serverconf]("serverconf")
    logger.info(s"Имя сервиса: ${serverconf.servicename}")
    logger.info(s"Версия сервиса: ${serverconf.version}")


  } else Play.stop(injector.instanceOf[Application])


  //тест чтения конфигурации TCP серверов и отладка серверов
  /*
  if (config.has("tcp-servers")) {
    val tcpconf: TcpConf = config.get[TcpConf]("tcp-servers")
    logger.info(s"Конфигурация TCP серверов   $tcpconf")

    val servers: Seq[ActorRef] = tcpconf.servers.map{
      confServer =>
        val serv = tcpBuilder.openServer(  confServer.port, confServer.id, confServer.phisicalObject, confServer.channelName )
        serv
    }

    val task: java.lang.Runnable = () => {
      servers.foreach( _ ! Message(s"Тест TCP\n") )
    }

    val scheduler: Scheduler = system.scheduler
    val dispatcher = system.dispatcher
    scheduler.scheduleWithFixedDelay(1 seconds, 5 seconds)(task)(dispatcher)
}
*/


  override def udpDebugCommand(command: String): Unit = {
  val js = Json.parse(command)

    (js \ "command").asOpt[String] match {
      case Some(command) => command match {
        case "init" =>  tcpClientsManager.init()
        case "stop" =>  tcpClientsManager.stop()
        case "setdata" =>
          val idOpt =  (js \ "id").asOpt[String]
          val dataOpt =  (js \ "data").asOpt[String]
          (idOpt, dataOpt) match {
            case (Some(id), Some(data)) => tcpClientsManager.setData(id,data)
            case _ => logger.warn(s"Некорректный формат команды ${Json.prettyPrint(js)}")
          }
        case "repeat" =>
          val delayOpt = (js \ "delay").asOpt[Int]
          delayOpt match {
            case Some(delay) => tcpClientsManager.repeat(delay)
            case _  => logger.warn(s"Некорректный формат команды ${Json.prettyPrint(js)}")
          }
        case "norepeat" => tcpClientsManager.norepeat()
        case "cardresponse" =>
            val phisicalObjectOpt = (js \ "phisicalObject").asOpt[String]
          phisicalObjectOpt match {
            case None =>
            case Some(po) =>
              //val dispatcherOpt = dispatchers.getPhisicalObjectByName(po)
              val dispatcherOpt = dispatchers.getPhisicalObjectByNameT(po)
              dispatcherOpt match {
                case None =>
                //case Some(dispatcher) => dispatcher !   CardResponse(po)
                case Some(dispatcher) => dispatcher !   PhisicalObjectTyped.CardResponse(po)
              }
          }

        case s:String => logger.warn(s"Неизвестная команда $s")
       }
      case None => logger.warn("Не обнаружено поле command")
    }



  }

  override def helloFromService(service: String): Unit = {
    logger.info(s"Привет из сервиса $service")
  }

  override def connected(c: Tcp.Connected): Unit = {
    println(s"Соединение к ${c.remoteAddress.getAddress}:${c.remoteAddress.getPort}")
  }

  override def message(s: String): Unit = {
    println(s)
  }

  override def data(d: ByteString, s: String): Unit = {
        println(s + " " + d.utf8String)
  }






  /*
  private var client:ActorRef = _
  private val taskClient: java.lang.Runnable = () => {
    val remoteSocket = new InetSocketAddress("127.0.0.1", 8877)
    client  = system.actorOf(TcpClient.props(remoteSocket, this), "client1")
    client ! ConnectToServer
  }

  private val  taskSend: java.lang.Runnable = () => {
    client  ! ByteString("v++++  4000%0000.")
  }



  private val  scheduler: Scheduler = system.scheduler
  implicit val disp: ExecutionContextExecutor = system.dispatcher

  scheduler.scheduleOnce(10 seconds, taskClient)
  scheduler.scheduleWithFixedDelay(13 seconds, 10 milliseconds )(taskSend)
  */


}





object ApplicationStartDebug {




}


