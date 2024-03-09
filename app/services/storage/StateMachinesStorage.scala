package services.storage

import akka.actor.typed.ActorRef
import play.api.Logger
import services.businesslogic.statemachines.oldrealisation.StateMachine
import services.businesslogic.statemachines.typed.StateMachineTyped.{StateMachineCommand, StatePlatform}
import services.storage.StateMachinesStorage.StateMachineAddException

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import scala.jdk.CollectionConverters.CollectionHasAsScala

object StateMachinesStorage {
  protected class StateMachineAddException(s: String) extends Exception(s)
}

@Singleton
class StateMachinesStorage {
  val logger: Logger = Logger(this.getClass)
  logger.info("Загружен StateMachinesStorage")

  // старое под объекты
  private val storage: ConcurrentHashMap[String, StateMachine] = new ConcurrentHashMap()

  def add(name: String, stm: StateMachine): Unit = {
      if (storage.containsKey(name))
        throw  new StateMachineAddException(s"Ошибка добавления стэйт-машины в хранилище с дублирующимся именем $name")

      storage.put(name, stm)
  }

  def get(name: String): Option[StateMachine] = if (storage.containsKey(name)) Some(storage.get(name)) else None

  def getList: List[(String,StateMachine)] = storage.entrySet().asScala.map(x => x.getKey -> x.getValue).toList





  //новое под акторы
  private val storageT: ConcurrentHashMap[String, ActorRef[StateMachineCommand]] = new ConcurrentHashMap()

  def addT(name: String, ref: ActorRef[StateMachineCommand]): ActorRef[StateMachineCommand] = {
    if (storageT.containsKey(name))
      throw  new StateMachineAddException(s"Ошибка добавления стэйт-машины в хранилище с дублирующимся именем $name")

    storageT.get(name, ref)
  }

  def getT(name: String): Option[ActorRef[StateMachineCommand]] = if (storageT.containsKey(name)) Some(storageT.get(name)) else None

  def getListT: List[(String, ActorRef[StateMachineCommand])] = storageT.entrySet().asScala.map(x => x.getKey -> x.getValue).toList


  //стейт платформ  для отдачи по http

  private val httpState: ConcurrentHashMap[String, (StatePlatform, Int)] = new ConcurrentHashMap()

  def getHttpState(name: String): Option[(StatePlatform, Int)] = if (httpState.containsKey(name)) Some(httpState.get(name)) else None

  def setHttpState(name: String, state: (StatePlatform, Int)): (StatePlatform, Int) = httpState.put(name, state)

}
