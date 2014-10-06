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
import ratpack.configuration.ConfigurationFactory
import spock.lang.Specification
import spock.lang.Subject

class DefaultConfigurationFactorySpec extends Specification {
  @Subject
  ConfigurationFactory configurationFactory = new DefaultConfigurationFactory()

  void "instantiates the specified class"() {
    when:
    def configSource = new DefaultConfigurationSource(new Properties(), new Properties())
    def config = configurationFactory.build(clazz, configSource)

    then:
    config.getClass() == clazz

    where:
    clazz << [Configuration, TestConfiguration]
  }
}
