package filters

import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class ExampleFilter @Inject()(implicit ec: ExecutionContext) extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = EssentialAction { request =>
    next(request).map { result =>
      result.withHeaders("X-ExampleFilter" -> "foo")
    }
  }
}