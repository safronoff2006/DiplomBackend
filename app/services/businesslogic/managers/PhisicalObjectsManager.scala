package services.businesslogic.managers

import akka.actor.ActorRef
import play.api.Logger

import services.businesslogic.dispatchers.PhisicalObject._
import javax.inject.{Inject, Named, Singleton}

@Singleton
class PhisicalObjectsManager @Inject()(@Named("RailWeighbridge") rail: ActorRef,
                                       @Named("TruckScale") truck1: ActorRef,
                                       @Named("TruckScale") truck2: ActorRef,
                                       @Named("TruckScale") truck3: ActorRef) {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен PhisicalObjectsManager")

  rail ! NameEvent("RailWeighbridge")
  truck1 ! NameEvent("TruckScale[1]")
  truck2 ! NameEvent("TruckScale[2]")
  truck3 ! NameEvent("TruckScale[3]")


  def getPhisicalObjectByName(name: String): Option[ActorRef] = {
    name match {
      case "RailWeighbridge" => Some(rail)
      case "TruckScale[1]" => Some(truck1)
      case "TruckScale[2]" => Some(truck2)
      case "TruckScale[3]" => Some(truck3)
      case _ => None
    }
  }

  def getValidNames(): List[String] = List("RailWeighbridge","TruckScale[1]", "TruckScale[2]", "TruckScale[3]")

}
