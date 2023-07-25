package models.configs



import com.typesafe.config.Config
import play.api.ConfigLoader

case class Serverconf(servicename: String, version: String)

object Serverconf {
  implicit val configLoader: ConfigLoader[Serverconf] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    Serverconf(
      servicename = if (config.hasPath("servicename")) config.getString("servicename") else "",
      version = if (config.hasPath("version")) config.getString("version") else ""
    )
  }
}
