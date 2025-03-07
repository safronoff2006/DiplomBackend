package services.start


import akka.actor.{ActorSystem, typed}
import akka.io.Tcp
import akka.util.ByteString
import models.configs.Serverconf
import models.db.DbModels.{Test, UidREF}
import models.readerswriters.WebModels.WebModelsWritesReads
import net.tcp._
import play.api.inject.{ApplicationLifecycle, Injector}
import play.api.libs.json.{JsValue, Json}
import play.api.{Application, Configuration, Logger, Play}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped
import services.businesslogic.managers.PhisicalObjectsManager
import services.businesslogic.statemachines.typed.StateMachineTyped
import services.businesslogic.statemachines.typed.StateMachineTyped.Stop
import services.db.DbLayer
import services.storage.StateMachinesStorage

import java.util.Locale
import javax.inject._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try, Using}

trait InterfaceStart {
  def udpDebugCommand(command: String): Unit
  def helloFromService(service:String):Unit
}

@Singleton
class ApplicationStartDebug @Inject()(lifecycle: ApplicationLifecycle, environment: play.api.Environment,
                                      injector: Injector, config: Configuration, tcpBuilder: TcpServerBuilder,
                                      implicit val system: ActorSystem, tcpClientsManager: TcpClientsManager,
                                      dispatchers: PhisicalObjectsManager, dbLayer: DbLayer, stateMashinesStorage: StateMachinesStorage)
  extends InterfaceStart with TcpClientOwner with WebModelsWritesReads {

  val logger: Logger = Logger(this.getClass)
  logger.info("Отладочная реализация ApplicationStart")
  // хук шатдауна
  lifecycle.addStopHook { () =>
    val mess = "Остановка Oilserver"
    logger.info(mess)

    val listStM: List[(String, typed.ActorRef[StateMachineTyped.StateMachineCommand])] = stateMashinesStorage.getListT

    listStM. foreach{x =>
      x._2 ! Stop
    }




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

//  val per: PerimetersSerialized = Perimeters('+','+','-','-')
//  private val jsPer =  Json.toJson(per)
//  println(s"jsPer    $jsPer")
//
//  private val serData: JsValue = Json.toJson(StatePlatformSerialized(10, "rail", per))
//  println(s"serData    $serData")

  private var listOfTests: List[Test] = List()
  (0 to 99).foreach { number =>
    val id  = java.util.UUID.randomUUID().toString
    listOfTests = listOfTests :+ Test(UidREF(id), s"Имя $id")
  }

  private val futureInsert = dbLayer.streamInsertTestFuture(listOfTests)

  private val rowsInserted = Await.result(futureInsert, Duration.Inf)

  logger. info(s"Inserted $rowsInserted   rows")


  //чтение основной конфигурации сервера
  if (config.has("serverconf")) {
    val serverconf: Serverconf = config.get[Serverconf]("serverconf")
    logger.info(s"Имя сервиса: ${serverconf.servicename}")
    logger.info(s"Версия сервиса: ${serverconf.version}")


  } else Play.stop(injector.instanceOf[Application])


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



}



object ApplicationStartDebug {




}


