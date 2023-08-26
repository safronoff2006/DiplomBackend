package models.readerswriters

import play.api.Logger
import play.api.libs.json.{ JsValue, Json}

import scala.util.Try


case class WorkplaceModel(workplaceId: String)

object WorkplaceModel {
  val logger: Logger = Logger(this.getClass)

  private class ParseJsValueToWorkplaceModelException(s: String) extends Exception(s)

  trait WorkplaceModelWritesReads {

    def validateWorkplace(js: JsValue): Try[WorkplaceModel] = Try {
      val optId: Option[String] = (js \ "workplaceId").asOpt[String]

      optId match {
        case None => throw new ParseJsValueToWorkplaceModelException(s"Ошибка парсинга json : ${Json.prettyPrint(js)} на кейс класс WorkplaceModel")
        case Some(id) => WorkplaceModel(id)
      }

    }
  }
}

