package models.extractors

trait Protocol {
  class ProtocolCreateException (s:String) extends Exception(s)

  val patternPrefix = "(v|V)".r


}

object Protocol2NoCard extends Protocol {
  def apply(prefix:String, perimeters: String) = {
    if (!patternPrefix.matches(prefix)) throw  new ProtocolCreateException(s"Не корректный префикс потокола")

    prefix }
}
