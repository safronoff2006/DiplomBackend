package services.businesslogic.statemachines.typed

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import com.google.inject.name.Named
import executioncontexts.CustomBlockingExecutionContext
import models.db.DbModels._
import models.extractors.Protocol2NoCard.{NoCard, patternPerimeters}
import models.extractors.Protocol2WithCard.WithCard

import models.extractors._
import play.api.Logger
import services.businesslogic.statemachines.typed.AutoStateMachineTyped.{Perimeters, StateAutoPlatform}
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.db.DbLayer
import services.db.DbLayer.InsertConf
import services.storage.GlobalStorage.{CreateAutoStateMachine, MainBehaviorCommand}
import services.storage.{GlobalStorage, StateMachinesStorage}
import utils.{AtomicOption, EmMarineConvert}

import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object AutoStateMachineTyped {
  case class Perimeters(in: Char, out: Char, left: Char, right: Char)

  case class StateAutoPlatform(perimeters: Perimeters, weight: Int, svetofor: String) extends StatePlatform

  object StateAutoPlatform {
    private class ParsePerimetersException(s: String) extends Exception(s)

    def apply(perimeters: String, weight: Int, svetofor: String): StateAutoPlatform = {
      Try {
        if (!patternPerimeters.matches(perimeters)) throw new ParsePerimetersException(s"Не верный формат периметров: $perimeters")
        val p: Perimeters = Perimeters(perimeters.charAt(0), perimeters.charAt(1),
          perimeters.charAt(2), perimeters.charAt(3))
        p

      } match {
        case Failure(exception) => StateAutoPlatform(Perimeters('?', '?', '?', '?'), weight, svetofor)
        case Success(p) => StateAutoPlatform(p, weight, svetofor)
      }
    }
  }
}

class AutoStateMachineWraper @Inject()(@Named("CardPatternName") nameCardPattern: String,
                                       stateStorage: StateMachinesStorage,
                                       @Named("ConvertEmMarine") convertEmMarine: Boolean,
                                       @Named("CardTimeout") cardTimeout: Long,
                                       dbLayer: DbLayer,
                                       insertConf: InsertConf
                                      )(implicit ex: CustomBlockingExecutionContext) extends StateMachineWraper {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан AutoStateMachineWraper")


  val optsys: Option[ActorSystem[MainBehaviorCommand]] = GlobalStorage.getSys

  val trySys: Try[ActorSystem[MainBehaviorCommand]] = Try {

    val sys: ActorSystem[MainBehaviorCommand] = optsys match {
      case Some(v) =>
        logger.info("Найден ActorSystem[MainBehaviorCommand]")
        v
      case None =>
        logger.error("Не найден ActorSystem[MainBehaviorCommand]")
        throw new Exception("Не найден ActorSystem[MainBehaviorCommand]")
    }
    sys
  }

  override def create(): String = {
    trySys match {
      case Failure(exception) =>
        logger.error(exception.getMessage)
        ""
      case Success(sys) =>
        val id: String = java.util.UUID.randomUUID.toString
        sys ! CreateAutoStateMachine(nameCardPattern, stateStorage, convertEmMarine, cardTimeout, dbLayer, insertConf, ex, id)
        id
    }

  }
}

class AutoStateMachineTyped(context: ActorContext[StateMachineCommand],
                            nameCardPattern: String,
                            stateStorage: StateMachinesStorage,
                            convertEmMarine: Boolean,
                            cardTimeout: Long,
                            dbLayer: DbLayer,
                            insertConf: InsertConf,
                            implicit val ex: CustomBlockingExecutionContext
                           )  extends StateMachineTyped(context: ActorContext[StateMachineCommand]) {

  loger.info("Создан актор -  стейт машина AutoStateMachine")
  loger.info(s"Паттерн карт: $nameCardPattern")

  override def register(name: String): Unit = stateStorage.addT(name, context.self)

  private val state: AtomicOption[StateAutoPlatform] = new AtomicOption(None)
  private val workedCard: AtomicOption[String] = new AtomicOption(None)


  private var listStatesToInsert: List[DbProtokol] = List()
  private var listPerimetersToInsert: List[DbPerimeters] = List()
  private var listCardsToInsert: List[DbCard] = List()


  //проанализировал - внутренний
  private def processingCard(): Unit = {
    workedCard.getState match {
      case Some(card) => loger.info(s"Процессинг карты $card")
      case None =>

    }
  }

  private var cardProcessingBusy = false

  //проанализировал - внутренний
  private def cardExecute(card: String): Unit = {
    val formatedCard = if (convertEmMarine) EmMarineConvert.emHexToEmText(card.toUpperCase)
    else card.toUpperCase

    if (!cardProcessingBusy) {
      cardProcessingBusy = true
      workedCard.setState(Some(formatedCard))
      processingCard()
    } else {
      loger.warn(s"$name - Card processing Busy")
    }
  }

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss SSS")


  //должно вызываться из кейса сообщения GetState
  override def getState: Option[StatePlatform] = state.getState

  //проанализировал - вызывается из обработки кейса сообщения  ProtocolExecute
  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: (String, String, String) = message match {
      case protocolObject: NoCard =>
        (protocolObject.perimeters, protocolObject.weight, protocolObject.svetofor)
      case protocolObject: WithCard =>
        context.self ! CardExecute(protocolObject.card)
        (protocolObject.perimeters, protocolObject.weight, protocolObject.svetofor)
      case _ => ("????", "??????", "?")
    }

    val perimeters: Perimeters = Perimeters(
      stateData._1.charAt(0),
      stateData._1.charAt(1),
      stateData._1.charAt(2),
      stateData._1.charAt(3)
    )

    val newState: StateAutoPlatform = StateAutoPlatform(perimeters, stateData._2.replace('?', '0').replace(' ', '0').toInt, stateData._3)

    state.setState(Some(newState))
    stateStorage.setHttpState(name, (newState, idnx))

  }


  override def onSignal: PartialFunction[Signal, Behavior[StateMachineCommand]]  = {
    case PostStop =>
      loger.info("AutoStateMachineTyped actor stopped")
      insertAccumulatedStates()
      this

  }



  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = {
    context.log.info("Create onMessage Behavior")

    msg match {

      case Stop =>
        context.log.info("AutoStateMachine stopped", name)
        Behaviors.stopped


      case Name(n) =>
        name = n
        loger.info("onMessage", "Name")
        work_

      case _ => Behaviors.same
    }


  }


  private val work: () => Behavior[StateMachineCommand] = () => Behaviors.receiveMessage[StateMachineCommand] { message =>
    context.log.info("Create work Behavior")
    message match {

      case Stop =>
        context.log.info("AutoStateMachine stopped", name)
        Behaviors.stopped

      case ProtocolExecute(mess) => protocolExecute(mess)
        loger.info(s"work ProtocolExecute  $name", "ProtocolExecute")
        val optHumanName = GlobalStorage.getOptionHumanNameScaleByName(name)
        val protocolWithName = ProtocolExecuteWithName(mess, name, optHumanName.getOrElse(""), idnx)
        sendStateToDB(protocolWithName)
        val respSend = StreamFeeder.send(protocolWithName)
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        work_

      case CardExecute(card) =>
        loger.info(s"work CardExecute  $name", "CardExecute")
        val cardWithName = CardExecuteWithName(card, name)
        sendCardToDB(cardWithName)
        val respSend = StreamFeeder.send(cardWithName)
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        withready_

      case GetState =>
        loger.info(s"work GetState $name    $getState", "GetState", "getState")
        work_

      case _ =>  Behaviors.unhandled

    }
  }

  private val work_  = work()

  private val withready: () => Behavior[StateMachineCommand] = () => Behaviors.withTimers[StateMachineCommand] { timers =>

    context.log.info("Create timeout Behavior")

    timers.startSingleTimer(Timeout, cardTimeout second)
    Behaviors.receiveMessagePartial {
      case Stop =>
        context.log.info("AutoStateMachine stopped", name)
        Behaviors.stopped

      case Flush =>
        loger.info("timeout  Flush", "Flush")
        workedCard.setState(None)
        cardProcessingBusy = false
        work_

      case message@Timeout =>
        loger.warn("timeout  Timeout!!!!!!", "Timeout")
        workedCard.setState(None)
        cardProcessingBusy = false
        val timeoutWithName = TimeoutWithName(name)
        sendCardToDB(timeoutWithName)
        val respSend = StreamFeeder.send(timeoutWithName)
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        work_



      case message@CardRespToState(param) =>
        loger.info(s"timeout  CardRespToState  $name  Обработка карты завершена. Параметр $param.")
        val cardWithName = CardRespToStateWithName(param, name)
        sendCardToDB(cardWithName)
        val respSend = StreamFeeder.send(cardWithName)
        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        workedCard.setState(None)
        cardProcessingBusy = false
        work_

      case _ =>  Behaviors.unhandled
    }

  }

  private val withready_ = withready()


  private def insertAccumulatedStates(): Unit = {
    val listStatesToInsertCopy = listStatesToInsert
    val listPerimetersToInsertCopy = listPerimetersToInsert
    listStatesToInsert = List()
    listPerimetersToInsert = List()

    val f1 = dbLayer.insertProtokolsFuture(listStatesToInsertCopy)
    f1.onComplete {
      case Failure(exception) =>
        loger.error(exception.getMessage)
      case Success(resInt) =>
        loger.info(s"Inserted $resInt  Protokols")
        val f2 = dbLayer.insertPerimetersFuture(listPerimetersToInsertCopy)
        f2.onComplete {
          case Failure(exception) => loger.error(exception.getMessage)
          case Success(resInt) => loger.info(s"Inserted $resInt  Perimeters")

        }
    }
  }

  private def sendStateToDB(state: StateMachineCommand): Unit = {

    val modified = Timestamp.from(Instant.now())
    state match {
      case obj: ProtocolExecuteWithName =>
        val objmessage: NoCardOrWithCard = obj.message


        val (prefix, weight, crc, optSvetofor) = objmessage match {
          case NoCard(prefix, perimeters, weight, crc, svetofor) => (prefix, weight.trim.toInt, crc, Some(svetofor))
          case WithCard(prefix, perimeters, weight, crc, card, typeCard, svetofor) => (prefix, weight.trim.toInt, crc, Some(svetofor))
          case _ => ("", 0, "", None)
        }



        val id: String = java.util.UUID.randomUUID().toString
        val dbprotokol: DbProtokol = DbProtokol(UidREF(id), obj.name, obj.humanName, obj.indx, prefix, weight, crc, optSvetofor, modified)

        listStatesToInsert = listStatesToInsert :+ dbprotokol

        val optPerimeters = objmessage match {
          case NoCard(prefix, perimeters, weight, crc, svetofor) => Some(DbPerimeters(UidREF(id), perimeters, modified))
          case ProtocolRail.RailWeight(prefix, weight) => None
          case WithCard(prefix, perimeters, weight, crc, card, typeCard, svetofor) => Some(DbPerimeters(UidREF(id), perimeters, modified))
          case _ => None
        }

        if (optPerimeters.isDefined) {
          listPerimetersToInsert = listPerimetersToInsert :+ optPerimeters.get
        }

        if (listStatesToInsert.size >= insertConf.state.listMaxSize ) {
          insertAccumulatedStates()
        }

      case _ =>

    }
  }


  private def insertAccumulatedCards(): Unit = {
    val listCardsToInsertCopy =  listCardsToInsert
    listCardsToInsert = List()

    val f = dbLayer.insertCardsFuture(listCardsToInsertCopy)
    f.onComplete {
      case Failure(exception) => loger.error(exception.getMessage)
      case Success(resInt) => loger.info(s"Inserted $resInt  Cards")
    }
  }

  private def sendCardToDB(card: StateMachineCommand): Unit = {
    val id: String = java.util.UUID.randomUUID().toString
    val modified = Timestamp.from(Instant.now())

    val optdbCard: Option[DbCard] = card match {
      case CardExecuteWithName(card, name) => Some(DbCard(UidREF(id), name, execute = true, resp = false, timeout = false, Some(card), None, modified))
      case CardRespToStateWithName(param, name) => Some(DbCard(UidREF(id), name, execute = false, resp = true,timeout = false, None, Some(param), modified))
      case TimeoutWithName(name) => Some (DbCard(UidREF(id), name, execute = false, resp = false, timeout = true, None, None, modified))
      case _ => None
    }

    if (optdbCard.isDefined) {
      listCardsToInsert = listCardsToInsert :+ optdbCard.get
    }

    if (listCardsToInsert.size >= insertConf.card.listMaxSize) {
      insertAccumulatedCards()
    }


  }


}
