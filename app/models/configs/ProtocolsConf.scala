package models.configs

object ProtocolsConf {

val SCALE_DATA_PATTERN_PROTOCOL1: String = "(v|V)("+
  "\\+|\\-|\\?){4}(\\?{6}|\\s{6}|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|"+
  "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|"+
  "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})" +
  "([0-9,]{12})?"

  val SCALE_DATA_PATTERN_PROTOCOL1_EMMARIN: String = "(v|V)("+
    "\\+|\\-|\\?){4}(\\?{6}|\\s{6}|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|"+
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|"+
    "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})" +
    "([0-9,]{12})"


  val SCALE_DATA_PATTERN_PROTOCOL2: String = "(v|V)("+
    "\\+|\\-|\\?){4}(\\?{6}|\\s{6}|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|"+
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}"+
    "|\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})" +
    "(((M[0-9a-fA-F]{8})|(Q[0-9a-fA-F\\-]{36})))?"+
    "%[0-9a-fA-F]{4}."

  val SCALE_DATA_PATTERN_PROTOCOL2_MIFARE: String = "(v|V)"+
    "(\\+|\\-|\\?){4}(\\?{6}|\\s{6}|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|"+
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|"+
    "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})" +
    "((M[0-9a-fA-F]{8})%[0-9a-fA-F]{4}.)"

  val SCALE_DATA_PATTERN_PROTOCOL2_QR: String = "(v|V)" +
    "(\\+|\\-|\\?){4}(\\?{6}|\\s{6}|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|" +
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|" +
    "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})" +
    "((Q[0-9a-fA-F\\-]{36})%[0-9a-fA-F]{4}.)"


  val SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN: String = "(v|V)" +
    "(\\+|\\-|\\?){4}(\\?{6}|\\s{6}|\\s{5}[0-9]{1}|\\s{4}-[0-9]{1}|\\s{4}[0-9]{2}|" +
    "\\s{3}-[0-9]{2}|\\s{3}[0-9]{3}|\\s{2}-[0-9]{3}|\\s{2}[0-9]{4}|\\s{1}-[0-9]{4}|" +
    "\\s{1}[0-9]{5}|\\-[0-9]{5}|[0-9]{6})" +
    "((M[0-9a-fA-F]{8})%[0-9a-fA-F]{4}.)"


  def getProtocolByName(name: String): String =  name match {
    case "SCALE_DATA_PATTERN_PROTOCOL1" => SCALE_DATA_PATTERN_PROTOCOL1
    case "SCALE_DATA_PATTERN_PROTOCOL1_EMMARIN" => SCALE_DATA_PATTERN_PROTOCOL1_EMMARIN
    case "SCALE_DATA_PATTERN_PROTOCOL2" => SCALE_DATA_PATTERN_PROTOCOL2
    case "SCALE_DATA_PATTERN_PROTOCOL2_MIFARE" => SCALE_DATA_PATTERN_PROTOCOL2_MIFARE
    case "SCALE_DATA_PATTERN_PROTOCOL2_QR" => SCALE_DATA_PATTERN_PROTOCOL2_QR
    case  "SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN" => SCALE_DATA_PATTERN_PROTOCOL2_EMMARIN

    case _ => ""
  }

}

