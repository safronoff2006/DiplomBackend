package models.configs

import com.typesafe.config.Config
import play.api.ConfigLoader

case class Udpconf(host: String, port: Int)

object Udpconf {
  implicit val configLoader: ConfigLoader[Udpconf] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    Udpconf(
      host = if (config.hasPath("host")) config.getString("host") else "",
      port = if (config.hasPath("port")) config.getInt("port") else 0

    )
  }
}
