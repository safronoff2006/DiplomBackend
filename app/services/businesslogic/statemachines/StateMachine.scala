package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import services.businesslogic.statemachines.StateMachine.StatePlatform

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import javax.inject.Inject

object StateMachine {
  trait StatePlatform
}

abstract class StateMachine @Inject() (implicit ex: CustomBlockingExecutionContext) {

  private[this] var _name: String = ""

  private var _idnx: Int = 0

  private def setIndx(name: String): Int = (name.indexOf("["), name.indexOf("]")) match {
      case (i1, i2) if i1 >= 0 && i2 > 0 => name.substring(i1 + 1, i2).toInt
      case _ => 0
  }

  def idnx: Int = _idnx

  def name: String = _name

  def register(name: String): Unit

  def getState: Option[StatePlatform]

  def name_=(value: String): Unit = {
    _name = value
    _idnx = setIndx(value)
    register(value)
  }

  private val queue:BlockingQueue[NoCardOrWithCard] = new LinkedBlockingQueue[NoCardOrWithCard]()

  def protocolMessage(message: NoCardOrWithCard): Unit = {
    queue.put(message)
  }

  def protocolExecute(message: NoCardOrWithCard):Unit

  ex.execute(() => {
    while (!Thread.interrupted()) {
      val message = queue.take()
     protocolExecute(message)
    }
  })

}
