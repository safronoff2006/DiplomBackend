package services.businesslogic.channelparsers

import akka.actor.ActorRef
import executioncontexts.CustomBlockingExecutionContext
import services.businesslogic.channelparsers.Parser.PatternInfo

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import javax.inject.Inject

object Parser {
    type PatternInfo = (String, String, String, String)
}

abstract class Parser @Inject() (implicit ex: CustomBlockingExecutionContext) {


  private var dispatcher: Option[ActorRef] = None

  protected var state: Int = 0

  protected val accumulator: StringBuilder = new StringBuilder("")

  protected var pattern: PatternInfo = ("","","","")

  private val queue:BlockingQueue[String] = new LinkedBlockingQueue[String]()

  def sendToParser(message:String): Unit = {
    queue.put(message)
  }

  def setDispatcher(ref: ActorRef): Unit = dispatcher = Some(ref)

  def setPattern(p: PatternInfo): Unit = pattern = p

  def getDispatcher: Option[ActorRef] = dispatcher

  protected val maxUnitLength = 50

  protected var unitCount: Int = 0

  protected def clearState(): Unit =  {
    accumulator.clear()
    unitCount = 0
    state = 0
  }

  protected def parse(message:String): Unit
  protected def compleatParseUnit(unit: String): Unit

  ex.execute(() => {
    while (!Thread.interrupted()) {
      val message = queue.take()
      parse(message)
    }
  })


}
