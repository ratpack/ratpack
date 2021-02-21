/*
 * Copyright 2014 the original author or authors.
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

import ratpack.exec.ExecController
import ratpack.exec.internal.DefaultExecController
import ratpack.http.client.HttpClient
import spock.lang.AutoCleanup

import java.time.Duration

class BlockingHttpClientSpec extends RatpackGroovyDslSpec {

  @AutoCleanup
  ExecController execController = new DefaultExecController(2)

  def "can use blocking http client"() {
    when:
    handlers {
      get {
        render "ok"
      }
    }

    and:
    def client = new BlockingHttpClient()

    then:
    client.request(HttpClient.of { }, applicationUnderTest.address, execController, Duration.ofSeconds(5), {}).body.text == "ok"
  }

}
