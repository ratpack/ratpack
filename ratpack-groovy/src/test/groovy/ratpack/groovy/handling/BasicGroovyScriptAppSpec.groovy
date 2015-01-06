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
import ratpack.launch.LaunchConfigs
import ratpack.server.RatpackServer
import ratpack.server.ServerConfigBuilder
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class BasicGroovyScriptAppSpec extends RatpackGroovyScriptAppSpec {

  boolean compileStatic = false
  boolean development = false

  @Override
  EmbeddedApp createApplication() {
    new EmbeddedAppSupport() {
      //TODO-JOHN
      protected LaunchConfig createLaunchConfig() {
        LaunchConfigs.createWithBaseDir(getClass().classLoader, getRatpackFile().parentFile.toPath(), getLaunchConfigProperties())
      }

      @Override
      protected RatpackServer createServer() {
        RatpackServer.of { spec ->
          spec
            .config(ServerConfigBuilder.launchConfig(createLaunchConfig()).port(0).build())
            .build(new GroovyScriptFileHandlerFactory().&create)
        }
      }
    }
  }

  protected Properties getLaunchConfigProperties() {
    Properties properties = new Properties()
    properties.setProperty(LaunchConfigs.Property.HANDLER_FACTORY, GroovyScriptFileHandlerFactory.name)
    properties.setProperty(LaunchConfigs.Property.DEVELOPMENT, development.toString())
    properties.setProperty(LaunchConfigs.Property.PORT, "0")
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

  def "dangling handlers in scripts are reported"() {
    given:
    development = true // so error message is written to response

    when:
    script """
      ratpack {
        handlers {
          get {
            // no response
          }
        }
      }
    """


    then:
    // line 6 because of prefix added by script() method
    def string = "No response sent for GET request to / (last handler: closure at line 6 of ${ratpackFile.getCanonicalFile().toPath().toUri()})"
    text == string
  }

  def "ratpack.groovy throws Error subclass as top level statement"() {
    when:
    script """
      import static ratpack.groovy.Groovy.ratpack

      throw new Error("Error")

      ratpack {
        handlers {
          get {
            render "ok"
          }
        }
      }
    """

    then:
    !application.server.running
    !application.server.bindHost
    application.server.bindPort < 0
  }

  def "ratpack.groovy throws Error subclass in handlers block"() {
    when:
    script """
      import static ratpack.groovy.Groovy.ratpack

      ratpack {
        handlers {
          throw new Error("Error")
          get {
            render "ok"
          }
        }
      }
    """

    then:
    !application.server.running
    !application.server.bindHost
    application.server.bindPort < 0
  }
}
