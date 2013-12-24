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
import ratpack.groovy.internal.ClosureUtil
import ratpack.groovy.test.TestHttpClient
import ratpack.launch.LaunchConfig
import ratpack.server.RatpackServer
import ratpack.test.ApplicationUnderTest
import spock.lang.Specification

import static ratpack.groovy.test.TestHttpClients.testHttpClient

abstract class InternalRatpackSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder
  RatpackServer server
  boolean reloadable

  @Delegate
  TestHttpClient client = testHttpClient(new TestApplicationUnderTest()) {
    configureRequest(it)
  }

  class TestApplicationUnderTest implements ApplicationUnderTest {
    @Override
    URI getAddress() {
      startServerIfNeeded()
      def server = InternalRatpackSpec.this.server
      new URI("${server.scheme}://${server.bindHost}:${server.bindPort}/")
    }
  }

  def setup() {
    client.resetRequest()
  }

  void configureRequest(RequestSpecification requestSpecification) {
    // do nothing
  }

  abstract protected RatpackServer createServer(LaunchConfig launchConfig)

  abstract protected LaunchConfig createLaunchConfig()

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
    ClosureUtil.configureDelegateFirst(this, configurer)
  }

  void stopServer() {
    try {
      server?.stop()
    } catch (ignore) {
      // ignore
    }
    server = null
  }

  def cleanup() {
    stopServer()
  }

  protected void startServerIfNeeded() {
    if (!server) {
      server = createServer(createLaunchConfig())
      server.start()
    }
  }

}
