package models.extractors

import com.typesafe.config.ConfigFactory
import models.configs.ProtocolsConf
import play.api.Logger
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import utils.CRC16Modbus

import scala.util.matching.Regex


  trait Protocol {
  val logger: Logger = Logger(this.getClass)

  val useCRC: Boolean = if (ConfigFactory.load.hasPath("useCRC")) ConfigFactory.load.getBoolean("useCRC") else false

  //-------------------------- Автомобильные весы
  protected val patternPrefix: Regex = "[vV]".r
  val patternPerimeters: Regex = "[-+?]{4}".r
  protected val patternWeight: Regex = ("(\\?{6}|\\s{6}" +
    "|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|" +
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|" +
    "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})").r
  protected val patternCrc: Regex = "[0-9a-fA-F]{4}".r
  protected val protokol2: String = ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2")

  protected val protokol2Cards: List[String] = List(
    ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2_MIFARE"),
    ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN"),
    ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_PROTOCOL2_QR")
  )

  protected val pCard: Regex = "([0-9a-fA-F]{8})|([0-9a-fA-F\\-]{36})".r
  protected val pCardType: Regex = "[MQ]".r
  protected val pSvetofor:Regex = "[RG?]".r

  //--------------------------------------------------
  protected class ProtocolCreateException (s:String) extends Exception(s)

  //-------------------------- ЖД весы
  protected val patternRailPrefix:Regex = "=".r
  protected val patternRailWeight:Regex = "([0-9]{6}|-[0-9]{5}|[0-9]{7}|-[0-9]{6})".r
  protected val protokolRail: String = ProtocolsConf.getProtocolByName("SCALE_DATA_PATTERN_RAIL_PROTOCOL")

}

  trait NoCardOrWithCard
  object Protocol2NoCard extends Protocol {
  case class NoCard(prefix: String, perimeters: String, weight: String, crc: String, svetofor: String) extends NoCardOrWithCard with PhisicalObjectEvent

  def apply(prefix: String, perimeters: String, weight: String, crc: String, svetofor: String): String = {
    if (!patternPrefix.matches(prefix)) throw new ProtocolCreateException(s"Не корректный префикс потокола: $prefix")
    if (!patternPerimeters.matches(perimeters)) throw new ProtocolCreateException(s"Не корректные периметры потокола: $perimeters")
    if (!patternWeight.matches(weight)) throw new ProtocolCreateException(s"Не корректный вес потокола: $weight")
    if (!patternCrc.matches(crc)) throw new ProtocolCreateException(s"Не корректный CRC потокола: $crc")
    if (!pSvetofor.matches(svetofor)) throw new ProtocolCreateException(s"Не корректный светофор потокола: $crc")

    prefix + perimeters + weight + svetofor + "%" + crc + "."
  }
  def apply(obj: NoCard): String = apply(obj.prefix, obj.perimeters, obj.weight, obj.crc, obj.svetofor)

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



      val svetofor = parts(0).length match {
        case 11 => "?"
        case 12 => parts(0).substring(11,12)
        case _ => "?"
      }

      val crcLongSended: Long = java.lang.Long.parseLong(crc,16)
      val modbus = new CRC16Modbus
      modbus.reset()
      val left = parts(0) + "%"
      modbus.update(left.getBytes, 0, left.length)
      val crcLongCalculated: Long = modbus.getValue

      (useCRC, crcLongSended, crcLongCalculated) match {
        case (false, _, _) =>  Some(NoCard(prefix, perimeters, weight, crc, svetofor))
        case (true, c1, c2) if c1 == c2 => Some(NoCard(prefix, perimeters, weight, crc, svetofor))
        case _ => None
      }

    }
  }
}

  object Protocol2WithCard extends Protocol {
  case class WithCard(prefix: String, perimeters: String, weight: String, crc: String,
                      card: String, typeCard: String, svetofor: String) extends NoCardOrWithCard with PhisicalObjectEvent
  def apply(prefix: String, perimeters: String, weight: String, crc: String, card: String, typeCard: String, svetofor: String): String = {
    if (!patternPrefix.matches(prefix)) throw new ProtocolCreateException(s"Не корректный префикс потокола: $prefix")
    if (!patternPerimeters.matches(perimeters)) throw new ProtocolCreateException(s"Не корректные периметры потокола: $perimeters")
    if (!patternWeight.matches(weight)) throw new ProtocolCreateException(s"Не корректный вес потокола: $weight")
    if (!patternCrc.matches(crc)) throw new ProtocolCreateException(s"Не корректный CRC потокола: $crc")
    if (!pCard.matches(card)) throw new ProtocolCreateException(s"Не корректная карта потокола: $card")
    if (!pCardType.matches(typeCard)) throw new ProtocolCreateException(s"Не корректный  тип карты протокола: $typeCard")
    if (typeCard == "M" && card.length != 8) throw new  ProtocolCreateException(s"Не корректная карта потокола: $card")
    if (typeCard == "Q" && card.length != 36) throw new  ProtocolCreateException(s"Не корректная карта потокола: $card")
    if (!pSvetofor.matches(svetofor)) throw new ProtocolCreateException(s"Не корректный светофор потокола: $svetofor")
    prefix + perimeters + weight + typeCard + card + svetofor + "%" + crc + "."
  }
  def apply(obj:WithCard): String = apply(obj.prefix, obj.perimeters, obj.weight, obj.crc, obj.card, obj.typeCard, obj.svetofor)
  def unapply(str: String): Option[WithCard] = {
    val isProtocol = protokol2.r.matches(str)
    val isCard: Boolean = protokol2Cards
      .map(_.r.matches(str))
      .reduce((a, b) => a || b)

    if (isProtocol && isCard) {
      val parts: Array[String] = str split "%"
      val crc = parts(1).substring(0, parts(1).indexOf("."))
      val prefix = parts(0).substring(0, 1)
      val perimeters = parts(0).substring(1, 5)
      val weight = parts(0).substring(5, 11)

      val indexMQ = parts(0).indexOf("M") match {
        case i if i >= 0 => i
        case i if i < 0 => parts(0).indexOf("Q")
      }

      val simbolMQ: String = parts(0).substring(indexMQ, indexMQ + 1)

      val card = simbolMQ match {
        case "M" => parts(0).substring(indexMQ + 1, indexMQ + 9)
        case "Q" => parts(0).substring(indexMQ + 1, indexMQ + 37)
      }

      val svetofor = parts(0).takeRight(1) match {
        case str: String if str == "R" || str == "G"  => str
        case _ => "?"
      }

      val typeCard = parts(0).substring(indexMQ, indexMQ + 1)

      val crcLongSended: Long = java.lang.Long.parseLong(crc, 16)
      val modbus = new CRC16Modbus
      modbus.reset()
      val left = parts(0) + "%"
      modbus.update(left.getBytes, 0, left.length)
      val crcLongCalculated: Long = modbus.getValue

      (useCRC, crcLongSended, crcLongCalculated) match {
        case (false, _, _) => Some(WithCard(prefix, perimeters, weight, crc, card, typeCard, svetofor))
        case (true, c1, c2) if c1 == c2 => Some(WithCard(prefix, perimeters, weight, crc, card, typeCard, svetofor))
        case _ => None
      }

    } else None
  }
}

  object ProtocolRail extends Protocol  {
  case class RailWeight(prefix:String, weight:String)  extends NoCardOrWithCard with PhisicalObjectEvent

  def apply(prefix:String, weight:String):String = {
    if (!patternRailPrefix.matches(prefix)) throw new ProtocolCreateException(s"Не корректный префикс потокола: $prefix")
    if (!patternRailWeight.matches(weight)) throw new ProtocolCreateException(s"Не корректный вес потокола: $weight")
    prefix + weight + "."
  }

  def apply(obj: RailWeight):String = apply(obj.prefix, obj.weight)

  def unapply(str: String):Option[RailWeight] = {
    val isProtocol =protokolRail.r.matches(str)
    if (isProtocol) {
      val prefix = str.substring(0, 1)
      val weight = str.substring(1, str.indexOf("."))
      Some(RailWeight(prefix, weight))
    }
    else None
  }
}

