package controllers


import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import executioncontexts.CustomBlockingExecutionContext
import models.db.DbModels.{DbCard, ProtokolWithCards, Test}
import models.readerswriters.CardModel
import models.readerswriters.CardModel.CardModelWritesReads
import models.readerswriters.WebModels.{WebCard, WebModelsWritesReads, WebTest}
import models.readerswriters.WorkplaceModel.WorkplaceModelWritesReads
import play.api._
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.mvc._
import services.businesslogic.managers.PhisicalObjectsManager
import services.businesslogic.statemachines.typed.AutoStateMachineTyped._
import services.businesslogic.statemachines.typed.RailStateMachineTyped._
import services.businesslogic.statemachines.typed.StateMachineTyped.StatePlatform
import services.db.DbLayer
import services.storage.GlobalStorage.WebProtokol
import services.storage.{GlobalStorage, StateMachinesStorage}
import slick.basic.DatabasePublisher

import javax.inject._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MainController @Inject()(val cc: ControllerComponents, stateStorage: StateMachinesStorage,
                               phisManager: PhisicalObjectsManager, globalStor: GlobalStorage, webProtocol: WebProtokol, dbLayer: DbLayer)
                              (implicit ex: CustomBlockingExecutionContext)
  extends AbstractController(cc) with CardModelWritesReads with WorkplaceModelWritesReads with WebModelsWritesReads {


  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан MainController")


  def getState(name: String): Action[AnyContent] = Action.async {
    request =>
      Future {

        val optState: Option[(StatePlatform, Int)] = stateStorage.getHttpState(name)

        val jsState: JsValue = optState match {
          case Some(pair) =>
            val indx: Int = pair._2
            val state: StatePlatform = pair._1

            state match {
              case StateAutoPlatform(perimeters, weight, svetofor) =>
                val serializedData: StatePlatformSerializedWithIndexWithSvetofor =
                  StatePlatformSerializedWithIndexWithSvetofor(weight, "auto", PerimetersSerialized(perimeters), indx, svetofor)
                Json.toJson(serializedData)


              case StateRailPlatform(weight) =>
                val serializedData: StatePlatformWithIndexSerialized =
                  StatePlatformWithIndexSerialized(weight, "rail", PerimetersSerialized("?", "?", "?", "?"), indx)
                Json.toJson(serializedData)

              case _ => Json.obj("presentation" -> state.toString)
            }

          case None => Json.obj("none" -> s"Не найдено http-состояние по имени $name")
        }

        Ok(jsState)
      }
  }

  private def jsonStatesOfListStates(listStates: List[(String, (StatePlatform, Int))]): Result = {
    val isres: Seq[JsValue] = listStates
      .map(smPair => (smPair._1, smPair._2._1, smPair._2._2))
      .map { x =>
        val indx: Int = x._3
        val state: StatePlatform = x._2
        val name: String = x._1

        val stateJson = state match {
          case StateAutoPlatform(perimeters, weight, svetofor) =>
            val serializedData: StatePlatformSerializedWithSvetofor =
              StatePlatformSerializedWithSvetofor(weight, "auto", PerimetersSerialized(perimeters), svetofor)
            Json.toJson(serializedData)

          case StateRailPlatform(weight) =>
            val serializedData: StatePlatformSerialized =
              StatePlatformSerialized(weight, "rail", PerimetersSerialized("?", "?", "?", "?"))
            Json.toJson(serializedData)

          case _ => Json.obj("presentation" -> state.toString)
        }

        Json.obj(
          "name" -> x._1,
          "indx" -> indx,
          "humanName" -> globalStor.getHumanNameScaleByName(x._1),
          "state" -> stateJson
        )

      }

    val res = Json.obj("states" -> JsArray(isres))

    Ok(res)
  }

  def getAllStates: Action[AnyContent] = Action.async {
    request => Future(jsonStatesOfListStates(stateStorage.getListHttpStates))
  }

  def getListStates(name: List[String]): Action[AnyContent] = Action.async {
    request => Future(jsonStatesOfListStates(stateStorage.getListHttpStates.filter(x => name.contains(x._1))))
  }

  def getValidNames: Action[AnyContent] = Action { request: Request[AnyContent] =>
    val names = phisManager.getValidNames.map(JsString)
    Ok(JsArray(names))
  }


  /////////////////////////////////////////

  def card: Action[AnyContent] = Action.async {
    request =>
      val body = request.body
      val jsonBody = body.asJson

      Future {
        jsonBody
          .map { json =>
            val optCard: Try[CardModel] = validateCard(json)
            optCard match {
              case Failure(ex) => BadRequest(s"Ошибка парсинга ${ex.getMessage}")
              case Success(cardObject) =>
                logger.info(s"Получена карта ${cardObject.card} от рабочего места ${cardObject.workplaceId}")
                Ok(json.toString())
            }
          }
          .getOrElse {
            BadRequest("Ожидается application/json тело запроса")
          }

      }

  }

  def workplacePing: Action[AnyContent] = Action.async {
    request =>
      val body = request.body
      val jsonBody: Option[JsValue] = body.asJson

      Future {
        jsonBody
          .map { json =>
            val optPing = validateWorkplace(json)
            optPing match {
              case Failure(ex) => BadRequest(s"Ошибка парсинга ${ex.getMessage}")
              case Success(pingObject) =>
                logger.info(s"Пинг от рабочего места ${pingObject.workplaceId}")
                Ok(json.toString())
            }
          }
          .getOrElse {
            BadRequest("Ожидается application/json тело запроса")
          }

      }

  }

  ///////////////////////////////////////////

  def getProtokolConfig: Action[AnyContent] = Action.async { request => {
    Future {
      val jsonCongig: JsValue = Json.toJson(webProtocol)
      Ok(jsonCongig)
    }
  }
  }

  implicit def convertF(future: Future[Seq[Test]]): Future[Seq[WebTest]] = future.map {
    seq => seq.map(t => t: WebTest)
  }

  ////////////////////////////////////////// тесты Slick
  def getAllTests: Action[AnyContent] = Action.async { request => {
    val futureDb: Future[Seq[WebTest]] = dbLayer.getAllTest
    val futureConversion: Future[JsValue] = for {
      seq: Seq[WebTest] <- futureDb
      js <- Future(Json.toJson(seq))
    } yield js

    futureConversion transformWith {
      case Success(value) => Future(Ok(value))
      case Failure(tr) => Future(Status(500)(s"Server internal error ${tr.getMessage}"))
    }
  }
  }


  def getAllTestsWithStream: Action[AnyContent] = Action { implicit request =>
    val publisher: DatabasePublisher[Test] = dbLayer.getAllTestStream
    val testsSource: Source[JsValue, NotUsed] = Source.fromPublisher(publisher)
      .via(Flow[Test].map(x => x: WebTest))
      .via(Flow[WebTest].map(x => Json.toJson(x)))

    Ok.chunked(testsSource)

  }

  def getTestById(id: String): Action[AnyContent] = Action.async { request => {
    val futureDb: Future[Option[Test]] = dbLayer.getTestById(id)
    futureDb.map {
      case Some(value) => Json.toJson(value: WebTest)
      case None => Json.obj("error" -> true, "errorMessage" -> s"Не найден объект по идентификатору $id")
    }.map(Ok(_))
  }
  }

  def getTestByIdWithStream(id: String): Action[AnyContent] = Action { implicit request =>
    val publisher = dbLayer.getTestByIdWithStream(id)
    val testsSource: Source[JsValue, NotUsed] = Source.fromPublisher(publisher)
      .via(Flow[Test].map(x => x: WebTest))
      .via(Flow[WebTest].map(x => Json.toJson(x)))

    Ok.chunked(testsSource)
  }

  ///////////////// рабочее

  def getAllCards: Action[AnyContent] = Action { implicit request =>
    val publisher = dbLayer.getAllCardsS
    val cardsSource = Source.fromPublisher(publisher)
      .via(Flow[DbCard].map(x => x: WebCard))
      .via(Flow[WebCard].map(x => Json.toJson(x)))
    Ok.chunked(cardsSource)
  }

  def getCardById(id: String): Action[AnyContent] = Action { implicit request =>
    val publisher = dbLayer.getByIdCardS(id)
    val cardsSource = Source.fromPublisher(publisher)
      .via(Flow[DbCard].map(x => x: WebCard))
      .via(Flow[WebCard].map(x => Json.toJson(x)))
    Ok.chunked(cardsSource)
  }

  def getAllProtokols: Action[AnyContent] = Action { implicit request =>
    val publisher = dbLayer.getAllProtokolsWithPerimetersS
    val protokolSource = Source.fromPublisher(publisher)
      .via(Flow[ProtokolWithCards].map(x => x.toString() + "\n"))
    Ok.chunked(protokolSource)
  }

  def getProtokolById(id: String): Action[AnyContent] = Action { implicit request =>
    val publisher = dbLayer.getByIdProtokolsWithPerimetersS(id)
    val protokolSource = Source.fromPublisher(publisher)
      .via(Flow[ProtokolWithCards].map(x => x.toString() + "\n"))
    Ok.chunked(protokolSource)
  }

}