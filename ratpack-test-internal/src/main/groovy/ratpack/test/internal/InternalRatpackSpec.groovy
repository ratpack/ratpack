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

import com.jayway.restassured.specification.RequestSpecification
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.groovy.internal.Util
import ratpack.groovy.test.TestHttpClient
import ratpack.server.RatpackServer
import ratpack.test.ApplicationUnderTest
import ratpack.util.Action
import spock.lang.Specification

import static ratpack.groovy.test.TestHttpClients.testHttpClient

abstract class InternalRatpackSpec extends Specification {

  @Rule TemporaryFolder temporaryFolder
  RatpackServer server
  boolean reloadable

  @Delegate TestHttpClient client = testHttpClient(
    {
      startServerIfNeeded()
      new URI("${server.scheme}://${server.bindHost}:${server.bindPort}")
    } as ApplicationUnderTest,
    { configureRequest(it) } as Action<RequestSpecification>
  )

  def setup() {
    client.resetRequest()
  }

  void configureRequest(RequestSpecification requestSpecification) {
    // do nothing
  }

  abstract protected RatpackServer createServer()

  File file(String path) {
    prepFile(new File(getDir(), path))
  }

  String getDirPath() {
    dir.absolutePath
  }

  File getDir() {
    temporaryFolder.root
  }

  static File prepFile(File file) {
    assert file.parentFile.mkdirs() || file.parentFile.exists()
    file
  }

  void app(Closure<?> configurer) {
    stopServer()
    Util.configureDelegateFirst(this, configurer)
  }

  void stopServer() {
    server?.stop()
    server = null
  }

  def cleanup() {
    stopServer()
  }

  protected startServerIfNeeded() {
    if (!server) {
      server = createServer()
      server.start()
    }
  }


}
