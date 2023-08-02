package services.start

import akka.actor.ActorSystem
import models.configs.Serverconf
import net.tcp.TcpServerBuilder
import play.api.inject.{ApplicationLifecycle, Injector}
import play.api.libs.json.{JsValue, Json}
import play.api.{Application, Configuration, Logger, Play}

import java.util.Locale
import javax.inject._
import scala.concurrent.Future
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try, Using}

trait InterfaceStart

@Singleton
class ApplicationStartDebug @Inject()(lifecycle: ApplicationLifecycle, environment: play.api.Environment, injector: Injector, config: Configuration,tcpBuilder: TcpServerBuilder, implicit val system: ActorSystem) extends InterfaceStart {
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
//  if (config.has("tcp-servers")) {
//    val tcpconf: TcpConf = config.get[TcpConf]("tcp-servers")
//    logger.info(s"Конфигурация TCP серверов   $tcpconf")
//
//    val servers: Seq[ActorRef] = tcpconf.servers.map{
//      confServer =>
//        val serv = tcpBuilder.openServer(  confServer.port, confServer.id, confServer.phisicalObject, confServer.channelName )
//        serv
//    }

//    val task: java.lang.Runnable = () => {
//      servers.foreach( _ ! Message(s"Тест TCP\n") )
//    }
//
//    val scheduler: Scheduler = system.scheduler
//    val dispatcher = system.dispatcher
//    scheduler.scheduleWithFixedDelay(1 seconds, 5 seconds)(task)(dispatcher)
//}



}

