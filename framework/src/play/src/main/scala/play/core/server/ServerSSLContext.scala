package play.core.server

import scala.util.{ Failure, Try }
import javax.net.ssl.SSLContext
import play.server.api.SSLContextProvider
import play.api.PlayException

object ServerSSLContext {

  def loadCustomSSLContext(className: String, classLoader: ClassLoader): Try[SSLContext] = {
    Try(classLoader.loadClass(className).getConstructor().newInstance().asInstanceOf[SSLContextProvider])
      .map(_.createSSLContext())
      .recoverWith {
        case t: Throwable => Failure(new PlayException(
          "Cannot load SSL context provider",
          "Class [" + className + "] cannot be instantiated.",
          t))
      }
  }

}
