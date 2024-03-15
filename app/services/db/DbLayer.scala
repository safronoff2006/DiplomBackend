package services.db

import akka.actor.ActorSystem
import executioncontexts.CustomBlockingExecutionContext
import models.db.DbModels.{Test, UidREF}
import models.db.DbSchema

import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

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
  def getAllTest: Future[Seq[Test]] = db.run(test.sortBy(_.name).result)

  def getTestById(id: String): Future[Option[Test]] =  db.run(test.filter(_.id === UidREF(id)).result).map(_.headOption)

}