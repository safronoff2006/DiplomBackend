package models.db

import play.api.libs.json.{Json, OWrites, Reads}
import slick.lifted.MappedTo

import java.sql.Timestamp
import java.time.Instant

object DbModels {

  ///////////////// Exception
  class FieldValueException(message: String, cause: Throwable = null) extends Exception(message, cause)

  //noinspection ScalaWeakerAccess
  object FieldValueException {
    def apply(message: String): FieldValueException = new FieldValueException(message)
    def apply(message: String, cause: Throwable): FieldValueException = new FieldValueException(message, cause)
  }

  /////////////////// PK (Primary Key class)
  case class UidREF(value: String) extends AnyVal with MappedTo[String]

  object UidREF {
    def apply(value: String): UidREF = {
      if (value.length > 36) throw FieldValueException("Размер первичного ключа больше 36")
      new UidREF(value)
    }

    implicit val writes: OWrites[UidREF] = Json.writes[UidREF]
    implicit val reads: Reads[UidREF] = Json.reads[UidREF]

  }

  ///////////////// table data
  case class Test(id: UidREF, name: String)


  //////////////////////  DbProtokol data

  case class DbPerimeters(id: UidREF, value: String, modified:  Timestamp = Timestamp.from(Instant.now()) )

  case class DbProtokol(id: UidREF, name: String, humanName: String, indx: Int,
                        prefix: String, weight: Int, crc: String,
                        svetofor: Option[String], modified: Timestamp = Timestamp.from(Instant.now()) )



  ///////////////////// DbCard data

  case class DbCard(id: UidREF, name: String = "",
                    execute: Boolean = false, resp: Boolean = false, timeout: Boolean = false ,
                    card: Option[String], param: Option[String], modified: Timestamp = Timestamp.from(Instant.now()) )



  type ProtokolWithCards = (UidREF, Int, String, String, String, String, Option[String], Timestamp, Option[String])

}
