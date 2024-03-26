package services.businesslogic.managers

import akka.actor.typed.ActorRef
import play.api.Logger
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped._
import services.storage.GlobalStorage

import javax.inject.{Inject, Named, Singleton}

@Singleton
class PhisicalObjectsManager @Inject()(@Named("RailWeighbridge") rail: ActorRef[PhisicalObjectEvent],
                                         @Named("TruckScale") truck1: ActorRef[PhisicalObjectEvent],
                                         @Named("TruckScale") truck2: ActorRef[PhisicalObjectEvent],
                                         @Named("TruckScale") truck3: ActorRef[PhisicalObjectEvent]) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен PhisicalObjectsManager")

  logger.info(s"rail  $rail")
  logger.info(s"truck1  $truck1")
  logger.info(s"truck2  $truck2")
  logger.info(s"truck3  $truck3")


    rail ! NameEvent("RailWeighbridge")

    truck1 ! NameEvent("TruckScale[1]")
    truck2 ! NameEvent("TruckScale[2]")
    truck3 ! NameEvent("TruckScale[3]")


  def getPhisicalObjectByNameT(name: String): Option[ActorRef[PhisicalObjectEvent]] = {
    name match {
      case "RailWeighbridge" => Some(rail)
      case "TruckScale[1]" => Some(truck1)
      case "TruckScale[2]" => Some(truck2)
      case "TruckScale[3]" => Some(truck3)
      case _ => None
    }
  }

  def getValidNames: List[String] = GlobalStorage.getValidNames

}
