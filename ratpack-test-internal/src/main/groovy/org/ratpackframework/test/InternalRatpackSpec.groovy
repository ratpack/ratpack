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

package org.ratpackframework.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ratpackframework.groovy.Util
import org.ratpackframework.groovy.test.RequestingSpec
import org.ratpackframework.server.RatpackServer

abstract class InternalRatpackSpec extends RequestingSpec {

  @Rule TemporaryFolder temporaryFolder
  boolean reloadable

  RatpackServer server

  abstract protected RatpackServer createServer()

  @Override
  protected ApplicationUnderTest getApplicationUnderTest() {
    startServerIfNeeded()
    new ApplicationUnderTest() {
      URI getAddress() {
        new URI("http://${InternalRatpackSpec.this.server.bindHost}:${InternalRatpackSpec.this.server.bindPort}")
      }
    }
  }

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
