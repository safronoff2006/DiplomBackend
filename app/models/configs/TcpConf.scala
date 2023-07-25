package models.configs

import com.typesafe.config.Config
import play.api.{ConfigLoader, Logger}

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.{Failure, Success, Try}

sealed case class Server(id: String, port: Int, phisicalObject: String, channelName: String)

class DublicateId (s:String) extends Exception(s)
class DublicatePort (s:String) extends Exception(s)

case class TcpConf(hostip: String, servers: List[Server])

object TcpConf {
  val logger: Logger = Logger(this.getClass)

  implicit val configLoader: ConfigLoader[TcpConf] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    val hostip = if (config.hasPath("host-ip")) config.getString("host-ip") else "0.0.0.0"

    val servers: List[Server] = if (config.hasPath("servers")) {
      val listConfigs: List[Config] = config.getConfigList("servers").asScala.toList

      var ports: List[Int] = List()
      var ids: List[String] = List()

      val list1EitherServers: List[Either[Throwable, Server]] = listConfigs.map(x => {
        val tryParse: Try[Server] = Try {
          val id = x.getString("id")
          val port = x.getInt("port")
          val phisicalObject = x.getString("phisicalObject")
          val channelName = x.getString("channelName")

          if (ports.contains(port)) {
            throw new DublicatePort(s"Парсинг конфигурации Tcp Сервера. Дублируется порт $port")
          }

          if (ids.contains(id)) {
            throw new DublicatePort(s"Парсинг конфигурации Tcp Сервера. Дублируется ID $id")
          }

          ports = port :: ports
          ids = id :: ids

          Server(id, port, phisicalObject, channelName)
        }

        val eitherServer: Either[Throwable, Server] = tryParse match {
          case Success(value) => Right(value)
          case Failure(tr) => Left(tr)
        }

        eitherServer
      }
      )

      list1EitherServers.foreach {
        case Left(tr) => logger.error(tr.getMessage)
        case _ =>
      }

      val listServers: List[Server] = list1EitherServers.flatMap(_.toOption)

      listServers

    } else List()

    TcpConf(hostip, servers)

  }


}


