package models.configs

import com.typesafe.config.Config
import play.api.{ConfigLoader, Logger}

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.{Failure, Success, Try}

sealed case class Server(id: String, port: Int)

case class TcpConf(hostip: String, servers: List[Server])

object TcpConf {
  val logger: Logger = Logger(this.getClass)

  implicit val configLoader: ConfigLoader[TcpConf] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    val hostip = if (config.hasPath("host-ip")) config.getString("host-ip") else "0.0.0.0"

    val servers: List[Server] = if (config.hasPath("servers")) {
      val listConfigs: List[Config] = config.getConfigList("servers").asScala.toList

      val list1EitherServers: List[Either[Throwable, Server]] = listConfigs.map(x => {
        val tryParse: Try[Server] = Try {
          val id = x.getString("id")
          val port = x.getInt("port")
          Server(id, port)
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


