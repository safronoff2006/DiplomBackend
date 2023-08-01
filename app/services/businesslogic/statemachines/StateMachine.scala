package services.businesslogic.statemachines

import executioncontexts.CustomBlockingExecutionContext
import models.extractors.NoCardOrWithCard
import models.extractors.Protocol2NoCard.NoCard
import models.extractors.Protocol2WithCard.WithCard

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import javax.inject.Inject

abstract class StateMachine @Inject() (implicit ex: CustomBlockingExecutionContext) {

  private[this] var _name: String = ""

  def name: String = _name

  def name_=(value: String): Unit = {
    _name = value
  }

  private val queue:BlockingQueue[NoCardOrWithCard] = new LinkedBlockingQueue[NoCardOrWithCard]()

  def protocolMessage(message: NoCardOrWithCard): Unit = {
    queue.put(message)
  }

  def protocolExecute(message: NoCardOrWithCard):Unit

  ex.execute(() => {
    while (!Thread.interrupted()) {
      val message = queue.take()
      message match {
        case obj: NoCard => println(s"NO CARD: $obj на стейт машине $name")
        case obj: WithCard => println(s"WITH CARD: $obj  на стейт машине $name")
      }
    }
  })

}
