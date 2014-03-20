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

package ratpack.groovy.handling

import ratpack.groovy.launch.GroovyScriptFileHandlerFactory
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigFactory
import ratpack.test.embed.EmbeddedApplication
import ratpack.test.embed.LaunchConfigEmbeddedApplication
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class BasicGroovyScriptAppSpec extends RatpackGroovyScriptAppSpec {

  boolean compileStatic = false
  boolean reloadable = false

  @Override
  EmbeddedApplication createApplication() {
    new LaunchConfigEmbeddedApplication() {
      @Override
      protected LaunchConfig createLaunchConfig() {
        LaunchConfigFactory.createWithBaseDir(getClass().classLoader, getRatpackFile().parentFile.toPath(), getLaunchConfigProperties())
      }
    }
  }

  protected Properties getLaunchConfigProperties() {
    Properties properties = new Properties()
    properties.setProperty(LaunchConfigFactory.Property.HANDLER_FACTORY, GroovyScriptFileHandlerFactory.name)
    properties.setProperty(LaunchConfigFactory.Property.RELOADABLE, reloadable.toString())
    properties.setProperty(LaunchConfigFactory.Property.PORT, "0")
    properties.setProperty("other." + GroovyScriptFileHandlerFactory.COMPILE_STATIC_PROPERTY_NAME, compileStatic.toString())
    properties.setProperty("other." + GroovyScriptFileHandlerFactory.SCRIPT_PROPERTY_NAME, ratpackFile.name)
    return properties
  }

  def "can use script app"() {
    given:
    compileStatic = true

    when:
    script """
      ratpack {
        handlers {
          get {
            render "foo"
          }
        }
      }
    """

    then:
    text == "foo"
  }
}
