package controllers

import executioncontexts.CustomBlockingExecutionContext
import models.readerswriters.CardModel
import models.readerswriters.CardModel.CardModelWritesReads
import models.readerswriters.WorkplaceModel.WorkplaceModelWritesReads
import play.api._
import play.api.libs.json.{JsArray, JsString, JsValue}
import play.api.mvc._
import services.businesslogic.managers.PhisicalObjectsManager
import services.storage.{GlobalStorage, StateMachinesStorage}

import javax.inject._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MainController @Inject()(val cc: ControllerComponents, stateStorage: StateMachinesStorage,
                               phisManager: PhisicalObjectsManager, globalStor: GlobalStorage)
                              (implicit ex: CustomBlockingExecutionContext)
  extends AbstractController(cc) with CardModelWritesReads with WorkplaceModelWritesReads {



  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан MainController")

/*
  private def jsonStatesOfListStates(listStates: List[(String, StateMachine)]): Result = {
    val isres: Seq[JsValue] = listStates
      .map(smPair => (smPair._1, smPair._2.getState, smPair._2.idnx))
      .map {
        x =>
          val indx = x._3
          val state = x._2 match {
            case None =>
              val name = x._1
              Json.obj(
                "none" -> s"Не установлено состояние стейт-машины $name"
              )
            case Some(st) => st match {
              case StateAutoPlatform(perimeters, weight, svetofor) => Json.obj(
                "type" -> "auto",
                "weight" -> weight,
                "perimeters" -> Json.obj(
                  "in" -> perimeters.in.toString,
                  "out" -> perimeters.out.toString,
                  "left" -> perimeters.left.toString,
                  "right" -> perimeters.right.toString
                ),
                "svetofor" -> svetofor
              )
              case StateRailPlatform(weight) => Json.obj(
                "type" -> "rail",
                "weight" -> weight,
                "perimeters" -> Json.obj(
                  "in" -> "?",
                  "out" -> "?",
                  "left" -> "?",
                  "right" -> "?"
                )
              )

              case _ => Json.obj("presentation" -> st.toString)
            }
          }

          Json.obj(
            "name" -> x._1,
            "indx" -> indx,
            "humanName" -> globalStor.getHumanNameScaleByName(x._1),
            "state" -> state
          )

      }

    val res = Json.obj("states" -> JsArray(isres))
    Ok(res)
  }

  def getAllStates: Action[AnyContent] = Action.async {
    request: Request[AnyContent] =>

      Future(jsonStatesOfListStates(stateStorage.getList))
  }

  def getState(name: String): Action[AnyContent] = Action.async {
    request: Request[AnyContent] => {
      Future {

        val optMachine: Option[StateMachine] = stateStorage.get(name)
        val state: JsValue = optMachine match {
          case None => Json.obj("none" -> s"Не найдена стейт-машина по имени $name")
          case Some(stMachine) =>
            val indx = stMachine.idnx
            val optState = stMachine.getState
            optState match {
              case None => Json.obj(
                "none" -> s"Не установлено состояние стейт-машины $name",
                "indx" -> indx
              )
              case Some(StateAutoPlatform(perimeters, weight, svetofor)) => Json.obj(
                "indx" -> indx,
                "type" -> "auto",
                "weight" -> weight,
                "perimeters" -> Json.obj(
                  "in" -> perimeters.in.toString,
                  "out" -> perimeters.out.toString,
                  "left" -> perimeters.left.toString,
                  "right" -> perimeters.right.toString
                ),
                "svetofor" -> svetofor
              )

              case Some(StateRailPlatform(weight)) => Json.obj(
                "indx" -> indx,
                "type" -> "auto",
                "weight" -> weight,
                "perimeters" -> Json.obj(
                  "in" -> "?",
                  "out" -> "?",
                  "left" -> "?",
                  "right" -> "?"

                )
              )
              case _ => Json.obj("presentation" -> optState.get.toString)
            }
        }
        Ok(state)


      }
    }
  }

  def getListStates(name: List[String]): Action[AnyContent] = Action.async {
    request: Request[AnyContent] =>
      Future {
        jsonStatesOfListStates(stateStorage.getList.filter(x => name.contains(x._1)))
      }
  }


 */

  def getValidNames: Action[AnyContent] = Action { request: Request[AnyContent] =>
    val names = phisManager.getValidNames.map(JsString)
    Ok(JsArray(names))
  }

  def card:  Action[AnyContent] = Action.async {
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

}