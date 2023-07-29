package executioncontexts

import akka.actor.ActorSystem
import play.api.Logger
import play.api.libs.concurrent.CustomExecutionContext

import javax.inject.{Inject, Singleton}

@Singleton
class CustomBlockingExecutionContext  @Inject()(system: ActorSystem) extends CustomExecutionContext(system, "blocking-io-dispatcher") {
  val logger: Logger = Logger(this.getClass)
  logger.info("Создан контекст исполнения CustomBlockingExecutionContext")
}
