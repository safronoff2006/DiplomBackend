val eem: List[Either[Int,String]] = List(Left(1), Left(2), Right("efefefef"), Right("wfefef"))

eem.flatMap(_.toOption)
