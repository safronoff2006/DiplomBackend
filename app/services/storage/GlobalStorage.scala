package services.storage

import akka.actor.typed.{ActorRef, ActorSystem}
import controllers.WebSocketController.MapWsType
import models.connection.WebSocketConection
import play.api.Logger
import services.businesslogic.channelparsers.typed.ParserTyped.{ParserCommand, PatternInfo}
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent
import services.businesslogic.statemachines.typed.StateMachineTyped.StateMachineCommand

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import scala.jdk.CollectionConverters.CollectionHasAsScala

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


object GlobalStorage {

  private var optSys: Option[ActorSystem[MainBehaviorCommand]] = None

  trait MainBehaviorCommand

  case class CreateTruckScaleDispatcher(parser: ActorRef[ParserCommand], stateMachine: ActorRef[StateMachineCommand], mainProtocolPattern: PatternInfo, id: String) extends MainBehaviorCommand
  case class CreateRailWeighbridgeDispatcher(parser: ActorRef[ParserCommand], stateMachine: ActorRef[StateMachineCommand], mainProtocolPattern: PatternInfo, id: String) extends MainBehaviorCommand
  case class CreateAutoProtocolParser(id: String) extends MainBehaviorCommand
  case class CreateRailProtocolParser(id: String) extends MainBehaviorCommand

  case class CreateAutoStateMachine(nameCardPattern: String,
                                    stateStorage: StateMachinesStorage,
                                    convertEmMarine: Boolean,
                                    cardTimeout: Long, id: String) extends MainBehaviorCommand


  case class CreateRailStateMachine(stateStorage: StateMachinesStorage, id: String) extends MainBehaviorCommand





  def getValidNames: List[String] = List("RailWeighbridge","TruckScale[1]", "TruckScale[2]", "TruckScale[3]")

  def setSys(sys: ActorSystem[MainBehaviorCommand]): Unit = {
    optSys = Some(sys)
  }

  def getSys: Option[ActorSystem[MainBehaviorCommand]] = optSys

  private val dispatchersMap = new ConcurrentHashMap[String, ActorRef[PhisicalObjectEvent]]()

  def setRefPOE(id: String, ref: ActorRef[PhisicalObjectEvent]): ActorRef[PhisicalObjectEvent] = dispatchersMap.put(id,ref)

  def getRefPOE(id:String): Option[ActorRef[PhisicalObjectEvent]] = if (dispatchersMap.containsKey(id)) Some(dispatchersMap.get(id)) else None

  private val parsersMap = new ConcurrentHashMap[String, ActorRef[ParserCommand]]()

  def setRefParser(id:String, ref: ActorRef[ParserCommand]): ActorRef[ParserCommand] = parsersMap.put(id, ref)

  def getRefParser(id: String): Option[ActorRef[ParserCommand]] = if (parsersMap.containsKey(id)) Some(parsersMap.get(id)) else None

  private val stateMachineMap = new ConcurrentHashMap[String, ActorRef[StateMachineCommand]]()

  def setRefSM(id: String, ref: ActorRef[StateMachineCommand]): ActorRef[StateMachineCommand] = stateMachineMap.put(id, ref)

  def getRefSM(id: String): Option[ActorRef[StateMachineCommand]] = if (stateMachineMap.containsKey(id)) Some(stateMachineMap.get(id)) else None

  private val connections: MapWsType = new MapWsType()

  def setConnection(id: String, connection: WebSocketConection): WebSocketConection = 
    connections.put(id, connection)
    
  def getConnection(id: String): Option[WebSocketConection] = if (connections.containsKey(id)) Some(connections.get(id)) else None
  
  private def getAllConnections: List[(String, WebSocketConection)] = connections.entrySet().asScala.map(x => x.getKey -> x.getValue).toList

  def removeConnection(id: String): Unit = if (connections.containsKey(id)) connections.remove(id)

  def sendToAllConnection(message: String): Unit = {
    getAllConnections.foreach{ connPair => connPair._2.out ! message
    }
  }

}
