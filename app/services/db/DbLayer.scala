package services.db

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import executioncontexts.CustomBlockingExecutionContext
import models.db.DbModels._
import models.db.DbSchema
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.db.DbLayer.InsertConf
import slick.basic.DatabasePublisher
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import javax.inject.Inject
import scala.concurrent.Future

object DbLayer {
  case class InsertInnerConf(listMaxSize: Int, groupSize: Int, parallelism: Int)
  case class InsertConf(test: InsertInnerConf, state: InsertInnerConf, card: InsertInnerConf)
}

//noinspection ScalaWeakerAccess
class DbLayer @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, dbShema: DbSchema, insertConf: InsertConf)
                       (implicit executionContext: CustomBlockingExecutionContext, implicit val system: ActorSystem)
                        extends HasDatabaseConfigProvider[JdbcProfile]{

  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен модуль работы с данными DbLayer")


  import profile.api._
  import dbShema._

  ////////////////////////////////////////тесты
 private  def allTestQuery = test.sortBy(_.name).result

  def getAllTest: Future[Seq[Test]] = db.run(allTestQuery)

  def getAllTestStream: DatabasePublisher[Test] = db.stream(allTestQuery.withStatementParameters(
      rsType = ResultSetType.ForwardOnly,
      rsConcurrency = ResultSetConcurrency.ReadOnly,
      fetchSize = 10000
  ).transactionally)

  private def getTestByIdQuery(id: String) = test.filter(_.id === UidREF(id)).result.map(_.headOption)
  private def getTestByIdQuery2(id: String) = test.filter(_.id === UidREF(id)).result

  def getTestById(id: String): Future[Option[Test]] =  db.run(getTestByIdQuery(id))

  def getTestByIdWithStream(id: String): DatabasePublisher[Test] = db.stream(getTestByIdQuery2(id).withStatementParameters(
    rsType = ResultSetType.ForwardOnly,
    rsConcurrency = ResultSetConcurrency.ReadOnly,
    fetchSize = 10000
  ).transactionally)

  private def insertTests(seq: Seq[Test]): Future[Int] = db.run(test ++= seq).map(_.getOrElse(0))
  def streamInsertTestFuture(listOfTest: List[Test]): Future[Int] =
    Source.fromIterator(() => listOfTest.iterator)
      .via(Flow[Test].grouped(insertConf.test.groupSize))
      .mapAsync(insertConf.test.parallelism)((tests: Seq[Test]) => insertTests(tests))
      .runWith(Sink.fold(0)(_+_))

  ////////////////////////////////// рабочие
  private def insertProtokols(seq: Seq[DbProtokol]): Future[Int] = db.run(protokol ++= seq).map(_.getOrElse(0))
  def insertProtokolsFuture(listProtokol: List[DbProtokol]): Future[Int] =
    Source.fromIterator(() => listProtokol.iterator)
      .via(Flow[DbProtokol].grouped(insertConf.state.groupSize))
      .mapAsync(insertConf.state.parallelism)((ps: Seq[DbProtokol]) => insertProtokols(ps))
      .runWith(Sink.fold(0)(_+_))

  private def insertPerimeters(seq: Seq[DbPerimeters]): Future[Int] = db.run(perimeters ++= seq).map(_.getOrElse(0))
  def insertPerimetersFuture(listPerimeters: List[DbPerimeters]): Future[Int] =
    Source.fromIterator(() => listPerimeters.iterator)
      .via(Flow[DbPerimeters].grouped(insertConf.state.groupSize))
      .mapAsync(insertConf.state.parallelism)((ps: Seq[DbPerimeters]) => insertPerimeters(ps))
      .runWith(Sink.fold(0)(_+_))

  private def insertCards(seq: Seq[DbCard]) = db.run(card ++= seq).map(_.getOrElse(0))
  def insertCardsFuture(listCard: List[DbCard]): Future[Int] =
    Source.fromIterator(() => listCard.iterator)
      .via(Flow[DbCard].grouped(insertConf.card.groupSize))
      .mapAsync(insertConf.card.parallelism)((ps: Seq[DbCard]) => insertCards(ps))
      .runWith(Sink.fold(0)(_+_))



  private def getAllProtokolsWithPerimetersQuery = for {
    (prot, per) <- protokol.sortBy(_.modified)  joinLeft perimeters on (_.id === _.id )
  } yield (prot.id, prot.weight, prot.crc, prot.prefix, prot.name, prot.humanName, prot.svetofor, prot.modified, per.map(_.value))


  def getAllProtokolsWithPerimetersF: Future[Seq[ProtokolWithCards]] =
    db.run(getAllProtokolsWithPerimetersQuery.result)

  def getAllProtokolsWithPerimetersS: DatabasePublisher[ProtokolWithCards] =
    db.stream{
      val result = getAllProtokolsWithPerimetersQuery.result
      result.withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 10000
      ).transactionally
    }


  //noinspection TypeAnnotation
  private def getByIdProtokolsWithPerimetersQuery(idd:String)  = for {
    (prot, per) <- protokol.filter(_.id === UidREF(idd)) joinLeft perimeters on (_.id === _.id)
  } yield (prot.id, prot.weight, prot.crc, prot.prefix, prot.name, prot.humanName, prot.svetofor, prot.modified, per.map(_.value))

  def getByIdProtokolsWithPerimetersF(idd: String): Future[Option[ProtokolWithCards]] =
    db.run(getByIdProtokolsWithPerimetersQuery(idd).result.headOption)

  def getByIdProtokolsWithPerimetersS(idd: String): DatabasePublisher[ProtokolWithCards] =
    db.stream{
      val result =  getByIdProtokolsWithPerimetersQuery(idd).result
      result.withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 10000
      ).transactionally
    }


  //noinspection TypeAnnotation
  private def getAllCardsQuery =  card.sortBy(_.modified).result

  def getAllCardsF: Future[Seq[DbCard]] = db.run(getAllCardsQuery)

  def getAllCardsS: DatabasePublisher[DbCard] = db.stream(getAllCardsQuery
    .withStatementParameters(
    rsType = ResultSetType.ForwardOnly,
    rsConcurrency = ResultSetConcurrency.ReadOnly,
    fetchSize = 10000
  ).transactionally)

  //noinspection TypeAnnotation
  private def getByIdCardQuery(idd: String) = card.filter(_.id === UidREF(idd)).result

  def getByIdCardF(idd: String): Future[Option[DbCard]] =  db.run(getByIdCardQuery(idd).headOption)

  def getByIdCardS(idd: String): DatabasePublisher[DbCard] = db.stream(getByIdCardQuery(idd)
    .withStatementParameters(
    rsType = ResultSetType.ForwardOnly,
    rsConcurrency = ResultSetConcurrency.ReadOnly,
    fetchSize = 10000
  ).transactionally)

}