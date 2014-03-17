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

import java.nio.file.Path

import static ratpack.launch.LaunchConfig.DEFAULT_MAX_CONTENT_LENGTH
import static ratpack.launch.LaunchConfig.DEFAULT_PORT
import static ratpack.launch.LaunchConfigFactory.Property.HANDLER_FACTORY
import static ratpack.launch.LaunchConfigFactory.Property.MAX_CONTENT_LENGTH
import static ratpack.launch.LaunchConfigFactory.createWithBaseDir

class LaunchConfigFactorySpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  Path baseDir
  def classLoader = this.class.classLoader
  def properties = new Properties()

  static class TestHandlerFactory implements HandlerFactory {
    @Override
    Handler create(LaunchConfig launchConfig) {
      new Handler() {
        void handle(Context context) throws Exception {

        }
      }
    }
  }

  def setup() {
    baseDir = temporaryFolder.newFolder().toPath()
    properties.setProperty(HANDLER_FACTORY, TestHandlerFactory.name)
  }

  def "can read values from properties"() {
    when:
    def launchConfig = createWithBaseDir(classLoader, baseDir, properties)

    then:
    launchConfig.maxContentLength == DEFAULT_MAX_CONTENT_LENGTH

    when:
    properties.setProperty(MAX_CONTENT_LENGTH, "20")
    launchConfig = createWithBaseDir(classLoader, baseDir, properties)

    then:
    launchConfig.maxContentLength == 20
  }

  def "PORT env var is respected"() {
    expect:
    createWithBaseDir(classLoader, baseDir, properties, [:]).port == DEFAULT_PORT
    createWithBaseDir(classLoader, baseDir, properties, [PORT: "1234"]).port == 1234
    createWithBaseDir(classLoader, baseDir, p("port": "5678"), [PORT: "1234"]).port == 5678

    when:
    createWithBaseDir(classLoader, baseDir, properties, [PORT: "abc"])

    then:
    def e = thrown LaunchException
    e.cause instanceof NumberFormatException
  }

  Properties p(Map<Object, Object> m) {
    def p = new Properties(properties)
    p.putAll(m)
    p
  }

}
