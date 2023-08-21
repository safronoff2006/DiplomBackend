package services.storage

import play.api.Logger

import javax.inject.Singleton

@Singleton
class GlobalStorage {
  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен GlobalStorage")

  private val mapHumanNamesScale: Map[String, String] = Map(
    "TruckScale[1]" -> "Автомобильные Весы 1",
    "TruckScale[2]" -> "Автомобильные Весы 2",
    "TruckScale[3]" -> "Автомобильные Весы 3",
    "RailWeighbridge" -> "ЖД Весы"
  )


  private def getHumanNamesScales: Map[String, String] = {
    mapHumanNamesScale  // только в режиме текста, должно брать из базы данных
  }
  private def getKeysOfName: Seq[String] = getHumanNamesScales.toList.map(_._1)

  private def getOptionHumanNameScaleByName(name: String): Option[String] = getHumanNamesScales.get(name)

  def getHumanNameScaleByName(name: String):String = getOptionHumanNameScaleByName(name) match {
    case Some(humanName) => humanName
    case None =>
      if (getKeysOfName.contains(name)) name else "Неизвестное имя"
  }



}
