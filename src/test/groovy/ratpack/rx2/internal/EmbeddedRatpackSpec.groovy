package ratpack.rx2.internal

import io.netty.util.CharsetUtil
import io.netty.util.ResourceLeakDetectorFactory
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.http.client.RequestSpec
import ratpack.rx2.internal.spock.InheritedTimeout
import ratpack.rx2.internal.spock.InheritedUnroll
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.Specification

import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicReference

@InheritedTimeout(30)
@InheritedUnroll
abstract class EmbeddedRatpackSpec extends Specification {

  private static final AtomicReference<Boolean> LEAKED = new AtomicReference<>(false)

  static {
    System.setProperty("io.netty.leakDetectionLevel", "paranoid")
    ResourceLeakDetectorFactory.resourceLeakDetectorFactory = new FlaggingResourceLeakDetectorFactory(LEAKED)
  }
  @Rule
  TemporaryFolder temporaryFolder

  @Delegate
  TestHttpClient client

  boolean failOnLeak = true

  abstract EmbeddedApp getApplication()

  void configureRequest(RequestSpec requestSpecification) {
    // do nothing
  }

  def setup() {
    LEAKED.set(false)
    client = testHttpClient({ application.address }) {
      configureRequest(it)
    }
  }

  def cleanup() {
    try {
      application.server.stop()
    } catch (Throwable ignore) {

    }

    if (LEAKED.get() && failOnLeak) {
      throw new Exception("A resource has leaked in this test!")
    }
  }

  String rawResponse(Charset charset = CharsetUtil.UTF_8) {
    StringBuilder builder = new StringBuilder()
    Socket socket = new Socket(application.address.host, application.address.port)
    try {
      new OutputStreamWriter(socket.outputStream, "UTF-8").with {
        write("GET / HTTP/1.1\r\n")
        write("Connection: close\r\n")
        write("\r\n")
        flush()
      }

      InputStreamReader inputStreamReader = new InputStreamReader(socket.inputStream, charset)
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader)

      def chunk
      while ((chunk = bufferedReader.readLine()) != null) {
        builder.append(chunk).append("\n")
      }

      builder.toString()
    } finally {
      socket.close()
    }
  }

}
