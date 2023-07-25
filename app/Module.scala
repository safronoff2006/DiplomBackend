import com.google.inject.AbstractModule
import play.api.Logger
import play.api.libs.concurrent.AkkaGuiceSupport

import services.start.{ApplicationStartDebug, InterfaceStart}

class Module  extends AbstractModule  with AkkaGuiceSupport {
  private val logger: Logger = Logger(this.getClass)
  logger.info("DEBUG Module")

  override def configure(): Unit = {
    logger.info("DEBUG Module configure")
    bind(classOf[InterfaceStart]).to(classOf[ApplicationStartDebug]).asEagerSingleton()
  }
}
