package models.db

import play.api.libs.json.{Json, OWrites, Reads}
import slick.lifted.MappedTo

object DbModels {

  ///////////////// Exception
  class FieldValueException(message: String, cause: Throwable = null) extends Exception(message, cause)

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




}
