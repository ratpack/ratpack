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

package ratpack.configuration.internal

import ratpack.configuration.Configuration
import ratpack.launch.LaunchConfig
import spock.lang.Specification
import spock.lang.Subject

class DefaultLaunchConfigFactorySpec extends Specification {
  @Subject
  DefaultLaunchConfigFactory launchConfigFactory = new DefaultLaunchConfigFactory()

  void "builds based on configuration"() {
    when:
    def defaultProperties = new Properties()
    def configSource = new DefaultConfigurationSource(getClass().classLoader, new Properties(), defaultProperties)
    def config = new Configuration()
    launchConfigFactory.handlerFactoryClass = TestHandlerFactory
    if (port) {
      launchConfigFactory.port = port
    }
    if (threads) {
      launchConfigFactory.threads = threads
    }
    def launchConfig = launchConfigFactory.build(configSource, config)

    then:
    launchConfig.port == expectedPort
    launchConfig.threads == expectedThreads

    where:
    port   | threads | expectedPort              | expectedThreads
    null   | null    | LaunchConfig.DEFAULT_PORT | LaunchConfig.DEFAULT_THREADS
    1234   | 42      | 1234                      | 42
  }
}
