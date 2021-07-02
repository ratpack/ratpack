/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.test.internal

import io.netty.util.CharsetUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.client.RequestSpec
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup

import java.nio.charset.Charset

abstract class EmbeddedRatpackSpec extends BaseRatpackSpec {

  @Rule
  TemporaryFolder temporaryFolder

  @Delegate
  TestHttpClient client

  @AutoCleanup
  EmbeddedApp otherApp

  boolean failOnLeak = true

  abstract EmbeddedApp getApplication()

  void configureRequest(RequestSpec requestSpecification) {
    // do nothing
  }

  def setup() {
    client = testHttpClient({ application.address }) {
      configureRequest(it)
    }
  }

  def cleanup() {
    try {
      application.server.stop()
    } catch (Throwable ignore) {

    }
  }

  EmbeddedApp otherApp(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    otherApp = GroovyEmbeddedApp.of {
      registryOf { add ServerErrorHandler, new DefaultDevelopmentErrorHandler() }
      handlers(closure)
    }
  }

  URI otherAppUrl(String path = "") {
    new URI("$otherApp.address$path")
  }

  String rawResponse(Charset charset = CharsetUtil.UTF_8) {
    Socket socket = socket()
    try {
      new OutputStreamWriter(socket.outputStream, "UTF-8").with {
        write("GET / HTTP/1.1\r\n")
        write("Connection: close\r\n")
        write("\r\n")
        flush()
      }

      socket.inputStream.getText(charset.name()).normalize()
    } finally {
      socket.close()
    }
  }

  Socket socket() {
    Socket socket = new Socket()
    socket.connect(new InetSocketAddress(application.address.host, application.address.port))
    socket
  }

  Socket withSocket(Socket socket = this.socket(), @DelegatesTo(OutputStream) Closure<?> closure) {
    def os = socket.outputStream
    new OutputStreamWriter(os, "UTF-8").with(closure)
    socket
  }
}
