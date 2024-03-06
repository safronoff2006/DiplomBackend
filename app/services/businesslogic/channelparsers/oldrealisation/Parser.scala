package services.businesslogic.channelparsers.oldrealisation

//старое нетепизированное

//новое типизированое
import executioncontexts.CustomBlockingExecutionContext
import services.businesslogic.channelparsers.oldrealisation.Parser.PatternInfo
import services.businesslogic.dispatchers.typed.PhisicalObjectTyped.PhisicalObjectEvent

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import javax.inject.Inject

object Parser {
    type PatternInfo = (String, String, String, String)
}

abstract class Parser @Inject() (implicit ex: CustomBlockingExecutionContext) {

  //старое нетипизированное
  private var dispatcher: Option[akka.actor.ActorRef] = None

  private var dispatcherT: Option[akka.actor.typed.ActorRef[PhisicalObjectEvent]] = None

  protected var state: Int = 0

  protected val accumulator: StringBuilder = new StringBuilder("")

  protected var pattern: PatternInfo = ("","","","")

  private val queue:BlockingQueue[String] = new LinkedBlockingQueue[String]()

  def sendToParser(message:String): Unit = {
    queue.put(message)
  }

  //старое нетипизированное
  def setDispatcher(ref: akka.actor.ActorRef): Unit = dispatcher = Some(ref)

  //новое типизированное
  def setDispatcherT(ref: akka.actor.typed.ActorRef[PhisicalObjectEvent]): Unit = dispatcherT = Some(ref)

  def setPattern(p: PatternInfo): Unit = pattern = p

  //старое нетипизированное
  def getDispatcher: Option[akka.actor.ActorRef] = dispatcher

  //новое типизированное
  def getDispatcherT: Option[akka.actor.typed.ActorRef[PhisicalObjectEvent]] = dispatcherT

  protected val maxUnitLength = 100

  protected var unitCount: Int = 0

  protected def clearState(): Unit =  {
    accumulator.clear()
    unitCount = 0
    state = 0
  }

  protected def parse(message:String): Unit


  ex.execute(() => {
    while (!Thread.interrupted()) {
      val message = queue.take()
      parse(message)
    }
  })


}
