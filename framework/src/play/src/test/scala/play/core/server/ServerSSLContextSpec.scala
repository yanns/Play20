package play.core.server


import org.specs2.mutable.Specification
import play.server.api.SSLContextProvider
import javax.net.ssl.SSLContext
import org.specs2.mock.Mockito

class WrongSSLContextProvider {}

class FailedSSLContextProvider extends SSLContextProvider {
  override def createSSLContext(): SSLContext = throw new Exception("test error while creating a SSL context")
}
class RightSSLContextProvider extends SSLContextProvider with Mockito {
  override def createSSLContext(): SSLContext = mock[SSLContext]
}

object ServerSSLContextSpec extends Specification {

  import ServerSSLContext.loadCustomSSLContext

  "ServerSSLContext" should {
    "fail to load a non existing SSLContextProvider" in {
      loadCustomSSLContext("blabla", this.getClass.getClassLoader) must beAFailedTry
    }

    "fail to load an existing SSLContextProvider with the wrong type" in {
      loadCustomSSLContext("play.core.server.WrongSSLContextProvider", this.getClass.getClassLoader) must beAFailedTry
    }

    "catch errors while creating an SSL context" in {
      loadCustomSSLContext("play.core.server.FailedSSLContextProvider", this.getClass.getClassLoader) must beAFailedTry
    }

    "load a custom SSLContext from a SSLContextProvider" in {
      loadCustomSSLContext("play.core.server.RightSSLContextProvider", this.getClass.getClassLoader) must beASuccessfulTry
    }
  }
}
