package models.readerswriters

import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import services.businesslogic.statemachines.typed.AutoStateMachineTyped.Perimeters
import services.businesslogic.statemachines.typed.StateMachineTyped.ProtocolExecuteWithName
import services.storage.GlobalStorage
import services.storage.GlobalStorage.WebProtokol

import scala.language.implicitConversions

object WebModels {
  val logger: Logger = Logger(this.getClass)

  trait WebModelsWritesReads {

    case class PerimetersSerialized(in: String, out: String, left: String, right: String)

    abstract class  StateSerialized {
      def weight: Int
      def `type`: String
      def perimeters: PerimetersSerialized
    }



    case class StatePlatformSerialized(weight: Int, `type`: String, perimeters: PerimetersSerialized) extends StateSerialized

    case class StatePlatformWithIndexSerialized(weight: Int, `type`: String, perimeters: PerimetersSerialized, indx: Int) extends StateSerialized

    case class StatePlatformSerializedWithSvetofor(weight: Int, `type`: String, perimeters: PerimetersSerialized, svetofor: String) extends StateSerialized

    case class StatePlatformSerializedWithIndexWithSvetofor(weight: Int, `type`: String, perimeters: PerimetersSerialized, indx: Int , svetofor: String) extends StateSerialized

    object PerimetersSerialized {
      def apply(per: Perimeters): PerimetersSerialized = PerimetersSerialized(
        per.in.toString,
        per.out.toString,
        per.left.toString,
        per.right.toString
      )
    }

    implicit def perimetersToSerialized(per: Perimeters): PerimetersSerialized = PerimetersSerialized(per)

    implicit val PerimetersWrites: Writes[PerimetersSerialized] = (
      (JsPath \ "in").write[String] and
        (JsPath \ "out").write[String] and
        (JsPath \ "left").write[String] and
        (JsPath \ "right").write[String]

      )(unlift(PerimetersSerialized.unapply))

    implicit val StatePlatformSerializedWrites: Writes[StatePlatformSerialized] = (
      (JsPath \ "weight").write[Int] and
        (JsPath \ "type").write[String] and
        (JsPath \ "perimeters").write[PerimetersSerialized]
      )(unlift(StatePlatformSerialized.unapply))

    implicit val StatePlatformSerializeWithIndexWrites: Writes[StatePlatformWithIndexSerialized] = (
      (JsPath \ "weight").write[Int] and
        (JsPath \ "type").write[String] and
        (JsPath \ "perimeters").write[PerimetersSerialized] and
        (JsPath \ "indx").write[Int]
        )(unlift(StatePlatformWithIndexSerialized.unapply))


    implicit val StatePlatformSerializedWithSvetoforWrites: Writes[StatePlatformSerializedWithSvetofor] = (
      (JsPath \ "weight").write[Int] and
        (JsPath \ "type").write[String] and
        (JsPath \ "perimeters").write[PerimetersSerialized] and
        (JsPath \ "svetofor").write[String]
      )(unlift(StatePlatformSerializedWithSvetofor.unapply))

    implicit val StatePlatformSerializeWithIndexSvetoforWrites: Writes[StatePlatformSerializedWithIndexWithSvetofor] = (
      (JsPath \ "weight").write[Int] and
        (JsPath \ "type").write[String] and
        (JsPath \ "perimeters").write[PerimetersSerialized] and
        (JsPath \ "indx").write[Int] and
        (JsPath \ "svetofor").write[String]
      )(unlift(StatePlatformSerializedWithIndexWithSvetofor.unapply))


    implicit val NoCardWrites: Writes[NoCard] = (
      (JsPath \ "prefix").write[String] and
        (JsPath \ "perimeters").write[String] and
        (JsPath \ "weight").write[String] and
        (JsPath \ "crc").write[String] and
        (JsPath \ "svetofor").write[String]
    ) (unlift (NoCard.unapply))


    implicit val WithCardWrites: Writes[WithCard] = (
      (JsPath \ "prefix").write[String] and
        (JsPath \ "perimeters").write[String] and
        (JsPath \ "weight").write[String] and
        (JsPath \ "crc").write[String] and
        (JsPath \ "card").write[String] and
        (JsPath \ "typeCard").write[String] and
        (JsPath \ "svetofor").write[String]
      )(unlift(WithCard.unapply))


    implicit val RailWeightWrites: Writes[RailWeight] = (
      (JsPath \ "prefix").write[String] and
        (JsPath \ "weight").write[String]
      ) (unlift (RailWeight.unapply))



    implicit def protocolExecuteWithNameToJson(obj: ProtocolExecuteWithName): JsValue = {
      val jsMessage: JsValue = obj.message match {
        case obj: NoCard => Json.toJson(obj)
        case obj: RailWeight => Json.toJson(obj)
        case obj: WithCard => Json.toJson(obj)
        case _ => Json.obj(
          "type" -> "error",
          "errorMessage" -> "Неверный подкласс  NoCardOrWithCard"
        )
      }
        val humanName = GlobalStorage.getOptionHumanNameScaleByName(obj.name).getOrElse("")
        Json.obj(
          "message" -> jsMessage,
          "name" -> obj.name,
          "humanName" -> humanName,
          "indx" -> obj.indx
        )
    }

    implicit val WebProtokolWrites: Writes[WebProtokol] = (
      (JsPath \ "protokol").write[String] and
        (JsPath \ "endPoint").write[String]
    ) (unlift (WebProtokol.unapply))



  }


}
