package models.readerswriters

import play.api.libs.json._

import java.sql.Timestamp
import java.text.SimpleDateFormat

trait TimestampFormat {
  implicit object timestampFormat extends Format[Timestamp] {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS")

    def reads(json: JsValue): JsResult[Timestamp] = {
      val str = json.as[String]
      JsSuccess(new Timestamp(format.parse(str).getTime))
    }

    def writes(ts: Timestamp): JsValue = JsString(format.format(ts))
  }
}

