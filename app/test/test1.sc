val eem: List[Either[Int,String]] = List(Left(1), Left(2), Right("efefefef"), Right("wfefef"))

eem.flatMap(_.toOption)


val lll: Seq[Option[String]] = Seq(None, Some("123"), Some("456"), None)

lll.flatten
