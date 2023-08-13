package controllers


import executioncontexts.CustomBlockingExecutionContext
import play.api._
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.mvc._
import services.businesslogic.managers.PhisicalObjectsManager
import services.businesslogic.statemachines.AutoStateMachine.StateAutoPlatform
import services.businesslogic.statemachines.StateMachine
import services.storage.{GlobalStorage, StateMachinesStorage}

import javax.inject._
import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class MainController @Inject()(val cc: ControllerComponents, stateStorage: StateMachinesStorage,
                               phisManager: PhisicalObjectsManager, globalStor: GlobalStorage)
                              (implicit ex: CustomBlockingExecutionContext )  extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан MainController")




  private def jsonStatesOfListStates(listStates: List[(String, StateMachine)]): Result = {
    val isres: Seq[JsValue]= listStates.map(smPair => (smPair._1, smPair._2.getState, smPair._2.idnx)).map {
      x =>
        val indx = x._3
        val state = x._2 match {
          case None =>
            val name = x._1
            Json.obj(
              "none" -> s"Не установлено состояние стейт-машины $name"
            )
          case Some(st) => st match {
            case StateAutoPlatform(perimeters, weight) => Json.obj(
              "type" -> "auto",
              "weight" -> weight,
              "perimeters" -> Json.obj(
                "in" -> perimeters.in.toString,
                "out" -> perimeters.out.toString,
                "left" -> perimeters.left.toString,
                "right" -> perimeters.right.toString
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
    request: Request[AnyContent] => Future (jsonStatesOfListStates(stateStorage.getList))
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
            case Some(StateAutoPlatform(perimeters, weight)) => Json.obj(
              "indx" -> indx,
              "type" -> "auto",
              "weight" -> weight,
              "perimeters" -> Json.obj(
                "in" -> perimeters.in.toString,
                "out" -> perimeters.out.toString,
                "left" -> perimeters.left.toString,
                "right" -> perimeters.right.toString
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
    request: Request[AnyContent] => Future {
      jsonStatesOfListStates(stateStorage.getList.filter(x => name.contains(x._1)))
    }
  }

  def getValidNames: Action[AnyContent] = Action { request: Request[AnyContent] =>
    val names = phisManager.getValidNames.map(JsString)
    Ok(JsArray(names))
  }
}