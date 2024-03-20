package services.businesslogic.statemachines.typed

import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import executioncontexts.CustomBlockingExecutionContext
import models.db.DbModels.{DbProtokol, UidREF}
import models.extractors.NoCardOrWithCard
import models.extractors.ProtocolRail.RailWeight
import play.api.Logger
import services.businesslogic.statemachines.typed.RailStateMachineTyped.StateRailPlatform
import services.businesslogic.statemachines.typed.StateMachineTyped._
import services.db.DbLayer
import services.db.DbLayer.InsertConf
import services.storage.GlobalStorage.MainBehaviorCommand
import services.storage.{GlobalStorage, StateMachinesStorage}
import utils.AtomicOption
import services.storage.GlobalStorage.CreateRailStateMachine

import java.sql.Timestamp
import java.time.Instant
import javax.inject.Inject
import scala.util.{Failure, Success, Try}

object RailStateMachineTyped {
  case class StateRailPlatform(weight: Int) extends StatePlatform
}

class RailStateMachineWraper @Inject()(stateStorage: StateMachinesStorage, dbLayer: DbLayer, insertConf: InsertConf)(implicit ex: CustomBlockingExecutionContext) extends StateMachineWraper {

  private val logger: Logger = Logger(this.getClass)
  logger.info("Создан RailStateMachineWraper")

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
        sys ! CreateRailStateMachine(stateStorage, dbLayer, insertConf, ex, id)
        id
    }


  }
}


class RailStateMachineTyped(context: ActorContext[StateMachineCommand], stateStorage: StateMachinesStorage, dbLayer: DbLayer, insertConf: InsertConf, implicit val ex: CustomBlockingExecutionContext)
  extends StateMachineTyped(context: ActorContext[StateMachineCommand]) {

  loger.info("Создан актор -  стейт машина RailStateMachineTyped")
  private val state: AtomicOption[StateRailPlatform] = new AtomicOption(None)

  override def register(name: String): Unit = stateStorage.addT(name, context.self)

  private var listStatesToInsert: List[DbProtokol] = List()


  override def getState: Option[StatePlatform] = state.getState


  override def onSignal: PartialFunction[Signal, Behavior[StateMachineCommand]] = {
    case PostStop =>
      loger.info("RailStateMachineTyped actor stopped")
      this

  }

  override def onMessage(msg: StateMachineCommand): Behavior[StateMachineCommand] = {
    msg match {

      case Stop =>
        context.log.info("RailStateMachine stopped", name)
        Behaviors.stopped

      case Name(n) =>
        name = n
        loger.info("onMessage", "Name")
        Behaviors.same

      case ProtocolExecute(message) => protocolExecute(message)
        loger.info("onMessage", "ProtocolExecute")
        val optHumanName = GlobalStorage.getOptionHumanNameScaleByName(name)
        val protokolWithName = ProtocolExecuteWithName(message, name, optHumanName.getOrElse(""), idnx)
        sendStatesToDB(protokolWithName)
        val respSend = StreamFeeder.send(protokolWithName)

        respSend match {
          case Left(exp) => context.log.error(exp.getMessage)
          case Right(value) => context.log.info(s"Send to stream: $value")
        }
        Behaviors.same

      case GetState =>
        loger.info("onMessage", "GetState", getState)
        Behaviors.same

      case _ => Behaviors.same
    }
  }

  override def protocolExecute(message: NoCardOrWithCard): Unit = {
    val stateData: String = message match {
      case protocolObject: RailWeight => protocolObject.weight
      case _ => "??????"
    }

    val newState: StateRailPlatform = StateRailPlatform(stateData.replace('?', '0').replace(' ', '0').toInt)
    state.setState(Some(newState))
    stateStorage.setHttpState(name, (newState, idnx))

  }

  private def insertAccumulatedStates(): Unit = {
    val listStatesToInsertCopy = listStatesToInsert
    listStatesToInsert = List()
    val f = dbLayer.insertProtokolsFuture(listStatesToInsertCopy)
    f.onComplete {
      case Failure(exception) => loger.error(exception.getMessage)
      case Success(resInt) => loger.info(s"Insert $resInt Protokols")
    }
  }

  private def sendStatesToDB(state: StateMachineCommand): Unit = {
    state match {
      case obj: ProtocolExecuteWithName =>

        val modified = Timestamp.from(Instant.now())
        val objmessage: NoCardOrWithCard = obj.message

        val (prefix, weight, crc, optSvetofor) = objmessage match {
          case RailWeight(prefix, weight) => (prefix, weight.trim.toInt, "", None)
          case _ => ("", 0, "", None)
        }

        val uuid = java.util.UUID.randomUUID().toString
        loger.info(s"uuid = $uuid")
        val dbprotokol: DbProtokol = DbProtokol(UidREF(uuid), obj.name, obj.humanName, obj.indx, prefix, weight, crc, optSvetofor, modified)
        loger.info(s"DbProtokol $dbprotokol")
        listStatesToInsert = listStatesToInsert :+ dbprotokol

        if (listStatesToInsert.size >= insertConf.state.listMaxSize) {
          insertAccumulatedStates()
        }

      case _ =>
    }
  }

}