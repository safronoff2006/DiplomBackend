package services.db

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import executioncontexts.CustomBlockingExecutionContext
import models.db.DbModels.{Test, UidREF}
import models.db.DbSchema
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.basic.DatabasePublisher
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import javax.inject.Inject
import scala.concurrent.Future

object DbLayer {

}

class DbLayer @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, dbShema: DbSchema)
                       (implicit executionContext: CustomBlockingExecutionContext, implicit val system: ActorSystem)
                        extends HasDatabaseConfigProvider[JdbcProfile]{

  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен модуль работы с данными DbLayer")


  import profile.api._
  import dbShema._

  //тесты
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

  def insertTests(seq: Seq[Test]): Future[Int] = db.run(test ++= seq).map(_.getOrElse(0))

  def streamInsertTestFuture(listOfTest: List[Test]): Future[Int] =
    Source.fromIterator(() => listOfTest.iterator)
      .via(Flow[Test].grouped(10))
      .mapAsync(5)((tests: Seq[Test]) => insertTests(tests))
      .runWith(Sink.fold(0)(_+_))

}