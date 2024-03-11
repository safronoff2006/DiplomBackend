package models.readerswriters

import play.api.Logger
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Writes}
import services.businesslogic.statemachines.typed.AutoStateMachineTyped.Perimeters

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





  }


}
