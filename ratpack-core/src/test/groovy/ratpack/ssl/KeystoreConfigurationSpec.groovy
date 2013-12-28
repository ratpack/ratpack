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
import org.junit.rules.TemporaryFolder
import ratpack.handling.Handler
import ratpack.launch.HandlerFactory
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigFactory
import spock.lang.Specification
import spock.lang.Unroll

import static ratpack.launch.LaunchConfigFactory.Property.HANDLER_FACTORY
import static ratpack.launch.LaunchConfigFactory.Property.SSL_KEYSTORE_FILE
import static ratpack.launch.LaunchConfigFactory.Property.SSL_KEYSTORE_PASSWORD

class KeystoreConfigurationSpec extends Specification {

  private static final String KEYSTORE_PATH = "ratpack/ssl/dummy.keystore"

  @Rule TemporaryFolder temporaryFolder

  static class NullHandlerFactory implements HandlerFactory {
    @Override
    Handler create(LaunchConfig launchConfig) throws Exception {
      return null
    }
  }

  @Unroll
  def "can configure SSL keystore using a keystore file property that is #description"() {
    given:
    Properties properties = new Properties()
    properties.setProperty SSL_KEYSTORE_FILE, keystoreFileProperty
    properties.setProperty SSL_KEYSTORE_PASSWORD, "password"
    properties.setProperty HANDLER_FACTORY, NullHandlerFactory.name

    when:
    def launchConfig = LaunchConfigFactory.createWithBaseDir(getClass().classLoader, temporaryFolder.root.toPath(), properties)

    then:
    launchConfig.getSSLContext() != null

    where:
    keystoreFileProperty          | description
    resourceAsFile(KEYSTORE_PATH) | "an absolute file path"
    resourceAsURL(KEYSTORE_PATH)  | "a URL"
    KEYSTORE_PATH                 | "a resource path"
  }

  private String resourceAsURL(String path) {
    getClass().getClassLoader().getResource(path).toString();
  }

  private String resourceAsFile(String path) {
    new File(getClass().getClassLoader().getResource(path).toURI()).absolutePath
  }
}
