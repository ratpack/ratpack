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

package ratpack.launch

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.handling.Context
import ratpack.handling.Handler
import spock.lang.Specification

import static ratpack.launch.LaunchConfigFactory.Property.HANDLER_FACTORY
import static ratpack.launch.LaunchConfigFactory.Property.MAX_CONTENT_LENGTH

// TODO: this test is well underdone, quick fix for issue #190 atm.
class LaunchConfigFactorySpec extends Specification {

  @Rule TemporaryFolder temporaryFolder

  static class TestHandlerFactory implements HandlerFactory {
    @Override
    Handler create(LaunchConfig launchConfig) {
      new Handler() {
        void handle(Context context) throws Exception {

        }
      }
    }
  }

  def "can read values from properties"() {
    given:
    def classLoader = this.class.classLoader
    def baseDir = temporaryFolder.newFolder()
    def properties = new Properties()
    properties.setProperty(HANDLER_FACTORY, TestHandlerFactory.name)

    when:
    def launchConfig = LaunchConfigFactory.createWithBaseDir(classLoader, baseDir, properties)

    then:
    launchConfig.maxContentLength == LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH

    when:
    properties.setProperty(MAX_CONTENT_LENGTH, "20")
    launchConfig = LaunchConfigFactory.createWithBaseDir(classLoader, baseDir, properties)

    then:
    launchConfig.maxContentLength == 20
  }

}
