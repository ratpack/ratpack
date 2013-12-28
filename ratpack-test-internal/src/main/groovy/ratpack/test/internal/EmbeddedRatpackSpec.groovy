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
import ratpack.groovy.test.TestHttpClient
import ratpack.test.embed.EmbeddedApplication
import spock.lang.Specification

import static ratpack.groovy.test.TestHttpClients.testHttpClient

abstract class EmbeddedRatpackSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  @Delegate
  TestHttpClient client

  abstract EmbeddedApplication getApplication()

  void configureRequest(RequestSpecification requestSpecification) {
    // do nothing
  }

  def setup() {
    client = testHttpClient({ application.address }) { configureRequest(it) }
  }

  def cleanup() {
    application.server.stop()
  }

}
