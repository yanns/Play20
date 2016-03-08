package play.api

import play.api.inject.guice.GuiceApplicationLoader
import play.utils.Reflect
import play.api.ApplicationLoader.Context

object DefaultApplicationLoader {

  /**
   * Locate and instantiate the ApplicationLoader.
   */
  def apply(context: Context): ApplicationLoader = {
    Reflect.configuredClass[ApplicationLoader, play.ApplicationLoader, GuiceApplicationLoader](
      context.environment, PlayConfig(context.initialConfiguration), "play.application.loader", classOf[GuiceApplicationLoader].getName
    ) match {
        case None =>
          new GuiceApplicationLoader
        case Some(Left(scalaClass)) =>
          scalaClass.newInstance
        case Some(Right(javaClass)) =>
          val javaApplicationLoader: play.ApplicationLoader = javaClass.newInstance
          // Create an adapter from a Java to a Scala ApplicationLoader. This class is
          // effectively anonymous, but let's give it a name to make debugging easier.
          class JavaApplicationLoaderAdapter extends ApplicationLoader {
            override def load(context: ApplicationLoader.Context): Application = {
              val javaContext = new play.ApplicationLoader.Context(context)
              val javaApplication = javaApplicationLoader.load(javaContext)
              javaApplication.getWrappedApplication
            }
          }
          new JavaApplicationLoaderAdapter
      }
  }

}