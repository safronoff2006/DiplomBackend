package models.db

import models.db.DbModels.{DbCard, DbPerimeters, DbProtokol, Test, UidREF}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
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

  //test
  class TestTable(tag: Tag) extends Table[Test](tag, "test") {
    def id = column[UidREF]("id", O.PrimaryKey, O.Length(36))
    def name = column[String]("name", O.Default(""), O.Length(100))
    def * = (id, name) <> (Test.tupled, Test.unapply)
  }

  val test = TableQuery[TestTable]

  //// DbProtokol схемы

  //noinspection ScalaWeakerAccess
  class ProtokolTable(tag: Tag) extends Table[DbProtokol](tag, "protokol") {
    def id = column[UidREF]("id", O.PrimaryKey, O.Length(36))
    def name = column[String]("name", O.Default(""), O.Length(200))
    def humanName = column[String]("humanName", O.Default(""), O.Length(200))
    def indx = column[Int]("indx", O.Default(0))
    def prefix = column[String]("prefix", O.Default("v"))
    def weight = column[Int]("weight", O.Default(0))
    def crc = column[String]("crc", O.Default(""))
    def svetofor = column[Option[String]]("svetofor")
    def modified = column[Timestamp]("modified", O.SqlType("timestamp with time zone   not null  default CURRENT_TIMESTAMP") )

    def * = (id, name, humanName, indx, prefix, weight, crc, svetofor, modified) <> (DbProtokol.tupled, DbProtokol.unapply )

  }

  val protokol = TableQuery[ProtokolTable]

  //noinspection ScalaWeakerAccess
  class PerimetersTable(tag: Tag) extends Table[DbPerimeters](tag, "perimeters") {
    def id = column[UidREF]("id", O.PrimaryKey, O.Length(36))
    def value = column[String]("value", O.Default("????"), O.Length(4))
    def modified = column[Timestamp]("modified", O.SqlType("timestamp with time zone   not null  default CURRENT_TIMESTAMP") )

    def * = (id, value, modified) <> (DbPerimeters.tupled, DbPerimeters.unapply)

    def ProtokolFK =
      foreignKey("protokol_fk", id, protokol)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
  }

  val perimeters = TableQuery[PerimetersTable]

  //noinspection ScalaWeakerAccess
  class CardTable(tag: Tag) extends Table[DbCard](tag, "card") {
    def id = column[UidREF]("id", O.PrimaryKey, O.Length(36))
    def name = column[String]("name", O.Default(""), O.Length(200))
    def execute = column[Boolean]("execute", O.Default(false))
    def resp = column[Boolean]("resp", O.Default(false))
    def timeout = column[Boolean]("timeout", O.Default(false))
    def card = column[Option[String]]("card", O.Length(50))
    def param = column[Option[String]]("param", O.Length(100))
    def modified = column[Timestamp]("modified", O.SqlType("timestamp with time zone   not null  default CURRENT_TIMESTAMP") )

    def * =  (id, name, execute, resp, timeout, card, param, modified) <> (DbCard.tupled, DbCard.unapply)

  }

  val card = TableQuery[CardTable]




}
