import scala.util.matching.Regex

Int.MaxValue

val pSvetofor:Regex = "[RG?]".r

pSvetofor.matches("W")

val s = "12345678R"

s.takeRight(1)
