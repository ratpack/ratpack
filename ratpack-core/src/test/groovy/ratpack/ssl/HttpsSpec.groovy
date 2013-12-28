/*
 * Copyright 2013 the original author or authors.
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

package ratpack.ssl

import org.junit.Rule
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.ssl.client.NonValidatingSSLClientContext

class HttpsSpec extends RatpackGroovyDslSpec {

  @Rule
  NonValidatingSSLClientContext clientContext = new NonValidatingSSLClientContext()

  def "can serve content over HTTPS"() {
    given:
    launchConfig {
      ssl HttpsSpec.getResource("dummy.keystore"), "password"
    }

    and:
    handlers {
      get {
        response.send "trust no one"
      }
    }

    expect:
    def address = applicationUnderTest.address
    address.scheme == "https"

    and:
    address.toURL().text == "trust no one"
  }
}


