import models.configs.ProtocolsConf
import play.api.Logger

trait Protocol {
  val logger: Logger = Logger(this.getClass)
  val patternPrefix = "[vV]".r
  val patternPerimeters = "[-+?]{4}".r
  val patternWeight = ("(\\?{6}|\\s{6}" +
    "|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|" +
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|" +
    "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})").r
  val patternCrc = "[0-9a-fA-F]{4}".r
  val protokol2 = ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2")
  val protokol2Cards: List[String] = List(
    ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2_MIFARE"),
    ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN"),
    ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2_QR")
  )

  class ProtocolCreateException (s:String) extends Exception(s)

  case class NoCard(prefix: String, perimeters: String, weight: String, crc: String)
  case class WithCard(prefix: String, perimeters: String, weight: String, crc: String, card: String, typeCard: Char)


}

object Protocol2NoCard extends Protocol {
  def apply(prefix: String, perimeters: String, weight: String, crc: String): String = {
    if (!patternPrefix.matches(prefix)) throw new ProtocolCreateException(s"Не корректный префикс потокола: $prefix")
    if (!patternPerimeters.matches(perimeters)) throw new ProtocolCreateException(s"Не корректные периметры потокола: $perimeters")
    if (!patternWeight.matches(weight)) throw new ProtocolCreateException(s"Не корректный вес потокола: $weight")
    if (!patternCrc.matches(crc)) throw new ProtocolCreateException(s"Не корректный CRC потокола: $crc")

    prefix + perimeters + weight + "%" + crc + "."
  }

  def unapply(str: String): Option[NoCard] = {
    val isProtocol = protokol2.r.matches(str)
    val isCard: Boolean = protokol2Cards
      .map(_.r.matches(str))
      .reduce((a, b) => a || b)

    if (!isProtocol || isCard)  None else {
      val parts: Array[String] = str split "%"
      val crc = parts(1).substring(0, parts(1).indexOf("."))
      val prefix = parts(0).substring(0, 1)
      val perimeters = parts(0).substring(1, 5)
      val weight = parts(0).substring(5, 11)
      Some(NoCard(prefix, perimeters, weight, crc))
    }
  }
}

object Protocol2WithCard extends Protocol {

}


Protocol2NoCard("v","--++","   100", "A0CF")

"v--++   100%B0CF."  match {
  case Protocol2NoCard(protokol) => println(protokol)
  case _ => println("Не протокол")
}

"v--+-   10000CF."  match {
  case Protocol2NoCard(protokol) => println(protokol)
  case _ => println("Не протокол")
}


"v--+?   100M00000000%00CF."  match {
  case Protocol2NoCard(protokol) => println(protokol)
  case _ => println("Не протокол")
}
