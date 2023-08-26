package models.readerswriters

import play.api.Logger
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsError, JsSuccess, JsPath, JsValue, Json, Reads, Writes}

import scala.util.Try

case class CardModel(card: String, workplaceId: String)

object CardModel {
  val logger: Logger = Logger(this.getClass)

  private class ParseJsValueToCardModelException(s: String) extends Exception(s)

  trait  CardModelWritesReads {

    implicit val cardModelWrites: Writes[CardModel] = (
      (JsPath \ "card").write[String]  and
        (JsPath \ "workplaceId").write[String]
    ) (unlift(CardModel.unapply))


    implicit val cardModelReads: Reads[CardModel] = (
      (JsPath \ "card").read[String] and
        (JsPath \ "workplaceId").read[String]
    ) (CardModel.apply _)

    def validateCard(js: JsValue): Try[CardModel] = Try {
      js.validate[CardModel] match {
        case JsError(errors) =>
          logger.error(errors.mkString)
          throw new ParseJsValueToCardModelException(s"Ошибка парсинга json : ${Json.prettyPrint(js)} на кейс класс CardModel")
        case JsSuccess(value, _)  => value
      }
    }

  }

}
