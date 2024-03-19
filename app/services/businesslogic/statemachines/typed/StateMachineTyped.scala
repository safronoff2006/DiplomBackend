package services.businesslogic.statemachines.typed

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.CompletionStrategy
import akka.stream.scaladsl.{Broadcast, Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import models.extractors.NoCardOrWithCard
import models.readerswriters.WebModels.WebModelsWritesReads
import org.slf4j.Logger
import play.api.libs.json.JsValue
import services.businesslogic.statemachines.typed.StateMachineTyped.{StateMachineCommand, StatePlatform, StreamFeeder}
import services.storage.GlobalStorage

object StateMachineTyped {
  trait StatePlatform

  trait StateMachineCommand



  case class Name(name: String) extends StateMachineCommand

  case class ProtocolExecute(message: NoCardOrWithCard) extends StateMachineCommand
  case class ProtocolExecuteWithName(message: NoCardOrWithCard, name: String, humanName: String, indx: Int) extends StateMachineCommand {
    def apply(obj: ProtocolExecute, name: String): ProtocolExecuteWithName = {
      val optHumanName = GlobalStorage.getOptionHumanNameScaleByName(name)
      ProtocolExecuteWithName(obj.message, name, optHumanName.getOrElse(""), indx)
    }
  }

  case class CardExecute(card: String) extends StateMachineCommand
  case class CardExecuteWithName(card: String, name: String) extends StateMachineCommand {
    def apply(obj: CardExecute, name: String): CardExecuteWithName = CardExecuteWithName(obj.card, name)
  }

  case class CardRespToState(param: String) extends StateMachineCommand
  case class CardRespToStateWithName(param: String, name: String) extends StateMachineCommand {
    def apply(obj: CardRespToState, name: String): CardRespToStateWithName = CardRespToStateWithName(obj.param, name)
  }


  case object GetState extends StateMachineCommand

  case object Flush extends StateMachineCommand

  case object Timeout extends StateMachineCommand
  case class  TimeoutWithName(name:String) extends StateMachineCommand


  object StreamFeeder extends WebModelsWritesReads {


    private case object First extends StateMachineCommand

    case object Emitted

    sealed trait EventStream


    private case class Element(content: StateMachineCommand) extends EventStream

    private case object ReachedEnd extends EventStream

    private case class FailureOccured(ex: Exception) extends EventStream



    private var optStreamActor: Option[ActorRef[EventStream]] = None

    private def runStream(ackReceiver: ActorRef[Emitted.type])(implicit system: ActorSystem[_]): ActorRef[EventStream] = {
      val source: Source[EventStream, ActorRef[EventStream]] =
        ActorSource.actorRefWithBackpressure[EventStream, Emitted.type](
          // get demand signalled to this actor receiving Ack
          ackTo = ackReceiver,
          ackMessage = Emitted,
          // complete when we send ReachedEnd
          completionMatcher = {
            case ReachedEnd => CompletionStrategy.draining
          },
          failureMatcher = {
            case FailureOccured(ex) => ex
          })

      val sourceCommand: Source[StateMachineCommand, ActorRef[EventStream]] = source
        .collect {
          case Element(msg) => msg
        }


      val sinkStates: Sink[StateMachineCommand,NotUsed] =   Flow[StateMachineCommand].filter {
        case s: ProtocolExecuteWithName => true
        case _ => false
      }.to(Sink.foreach { x =>
        system.log.info(s"Принято в KAFKA топик States: ${x.toString}")
      })

      val flowFilterState: Flow[StateMachineCommand, StateMachineCommand, NotUsed] = Flow[StateMachineCommand].filter {
        case s: ProtocolExecuteWithName => true
        case _ => false
      }

      val flowConverFilteredToJson: Flow[StateMachineCommand, String, NotUsed] = flowFilterState.map {
        case s: ProtocolExecuteWithName =>
          val json: JsValue = s
          json.toString()
        case _ => "{ \"type\":\"error\",  \"errorMessage\":\"Неверный класс для конвертации в JSON\" }"
      }

      val sinkCards: Sink[StateMachineCommand, NotUsed] = Flow[StateMachineCommand].filter {
        case _: CardExecuteWithName => true
        case _: CardRespToStateWithName => true
        case _: TimeoutWithName => true
        case _ => false
      }.to( Sink.foreach(x => system.log.info(s"Принято в KAFKA топик Cards: ${x.toString}")))


      val sinkWebSocket: Sink[StateMachineCommand, NotUsed] = flowConverFilteredToJson.to(Sink.foreach(x => GlobalStorage.sendToAllConnection(x)))

      val sink = Sink.combine(sinkStates, sinkCards, sinkWebSocket)(Broadcast[StateMachineCommand](_))

      val streamActor: ActorRef[EventStream] = sourceCommand
        .to(sink)
        .run()


      streamActor
    }

    def send(data: StateMachineCommand, optStreamSource: Option[ActorRef[EventStream]]  = None): Either[Throwable, StateMachineCommand] = {
      optStreamSource match {
        case Some(ref) =>
          ref ! Element(data)
          Right(data)
        case None => optStreamActor match {
          case Some(ref) =>
            ref ! Element(data)
            Right(data)
          case None => Left(new Exception("Не установлен StreamSource"))
        }
      }
    }

    def apply(): Behavior[Emitted.type] = Behaviors.setup { context =>
      val streamActor: ActorRef[EventStream] = runStream(context.self)(context.system)
      optStreamActor = Some(streamActor)
      context.log.info("Create stream actor")
      val resultSend  = send(First, optStreamActor)
      resultSend match {
        case Left(exc) => context.log.error(exc.getMessage)
        case Right(value) => context.log.info(s"Send to stream: $value")
      }
      Behaviors.same
    }


  }


}




abstract class StateMachineWraper(){
  def create(): String
}

abstract class StateMachineTyped(context: ActorContext[StateMachineCommand])
  extends AbstractBehavior[StateMachineCommand](context){

  protected val loger: Logger = context.log
  loger.info("Hello from StateMachineTyped!!!")



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

  def protocolExecute(message: NoCardOrWithCard):Unit

  override def onSignal: PartialFunction[Signal, Behavior[StateMachineCommand]] =  {
    case PostStop =>
      loger.info("StateMachineTyped actor stopped")
      this

  }

  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = ???

  ActorSystem(StreamFeeder(), "stream-feeder")

}
