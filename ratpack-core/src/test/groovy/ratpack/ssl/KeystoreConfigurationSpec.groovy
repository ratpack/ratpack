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
import ratpack.test.internal.RatpackGroovyScriptAppSpec
import ratpack.test.internal.ssl.client.NonValidatingSSLClientContext
import spock.lang.Unroll

import static ratpack.launch.LaunchConfigFactory.Property.SSL_KEYSTORE_FILE
import static ratpack.launch.LaunchConfigFactory.Property.SSL_KEYSTORE_PASSWORD

class KeystoreConfigurationSpec extends RatpackGroovyScriptAppSpec {

  private static final String KEYSTORE_PATH = "ratpack/ssl/dummy.keystore"

  Properties properties

  def setup() {
    properties = super.getProperties()
  }

  protected Properties getProperties() {
    properties
  }

  @Rule
  NonValidatingSSLClientContext clientContext = new NonValidatingSSLClientContext()

  @Unroll
  def "can configure SSL keystore using a keystore file property that is #description"() {
    given:
    stopServer()
    properties.setProperty SSL_KEYSTORE_FILE, keystoreFileProperty
    properties.setProperty SSL_KEYSTORE_PASSWORD, "password"

    and:
    app {
      script """
        ratpack {
          handlers {
            get {
              response.send "trust no one"
            }
          }
        }
      """
    }

    expect:
    def address = applicationUnderTest.address
    address.scheme == "https"

    and:
    address.toURL().text == "trust no one"

    where:
    keystoreFileProperty          | description
    resourceAsFile(KEYSTORE_PATH) | "an absolute file path"
    resourceAsURL(KEYSTORE_PATH)  | "a URL"
    KEYSTORE_PATH                 | "a resource path"
  }

  def "if a keystore is not configured with properties the server will not use SSL"() {
    given:
    app {
      script """
        ratpack {
          handlers {
            get {
              response.send "go ahead, read my traffic"
            }
          }
        }
      """
    }

    expect:
    def address = applicationUnderTest.address
    address.scheme == "http"

    and:
    address.toURL().text == "go ahead, read my traffic"
  }

  private String resourceAsURL(String path) {
    getClass().getClassLoader().getResource(path).toString();
  }

  private String resourceAsFile(String path) {
    new File(getClass().getClassLoader().getResource(path).toURI()).absolutePath
  }
}
