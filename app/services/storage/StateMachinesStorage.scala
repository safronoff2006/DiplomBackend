package services.storage

import play.api.Logger
import services.businesslogic.statemachines.StateMachine
import services.storage.StateMachinesStorage.StateMachineAddException

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

object StateMachinesStorage {
  protected class StateMachineAddException(s: String) extends Exception(s)
}

@Singleton
class StateMachinesStorage {
  val logger: Logger = Logger(this.getClass)
  logger.info("Загружен StateMachinesStorage")

  val storage: ConcurrentHashMap[String, StateMachine] = new ConcurrentHashMap()

  def add(name: String, stm: StateMachine): Unit = {
      if (storage.containsKey(name))
        throw  new StateMachineAddException(s"Ошибка добавления стэйт-машины в хранилище с дублирующимся именем $name")

      storage.put(name, stm)
  }

  def get(name: String): Option[StateMachine] = if (storage.containsKey(name)) Some(storage.get(name)) else None



}
