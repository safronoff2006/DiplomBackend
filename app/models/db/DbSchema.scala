package models.db

import models.db.DbModels.{Test, UidREF}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Try

@Singleton
class DbSchema  @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  private val logger: Logger = Logger(this.getClass)
  logger.info("Загружен модуль моделей данных Slick")

  import profile.api._

  ///////////// exec для тестов
  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 4 seconds)

  def tryExec[T](action: DBIO[T]): Try[T] = Try(Await.result(db.run(action), 4 seconds))

  def tryExecTransact[T](action: DBIO[T]): Try[T] = Try(Await.result(db.run(action.transactionally), 4 seconds))

  ////////// схемы таблиц

  class TestTable(tag: Tag) extends Table[Test](tag, "test") {
    def id = column[UidREF]("id", O.PrimaryKey, O.Length(36))
    def name = column[String]("name", O.Default(""), O.Length(100))
    def * = (id, name) <> (Test.tupled, Test.unapply)
  }

  val test = TableQuery[TestTable]


}
