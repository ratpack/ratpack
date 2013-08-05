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
package org.ratpackframework.handling.internal

import com.jayway.restassured.RestAssured
import com.jayway.restassured.specification.RequestSpecification
import org.ratpackframework.groovy.launch.GroovyScriptHandlerFactory
import org.ratpackframework.launch.LaunchConfigFactory
import org.ratpackframework.test.groovy.RatpackGroovyScriptAppSpec

class DefaultContextSpec extends RatpackGroovyScriptAppSpec {

  Properties properties

  @Override
  RequestSpecification createRequest() {
    //We are testing redirects so its best not to follow them
    RestAssured.with().urlEncodingEnabled(false).redirects().follow(false)
  }

  @Override
  protected Properties getProperties() {
    return properties
  }

  def setup() {
    properties = new Properties()
    properties.setProperty(LaunchConfigFactory.Property.HANDLER_FACTORY, GroovyScriptHandlerFactory.name)
    properties.setProperty(LaunchConfigFactory.Property.RELOADABLE, reloadable.toString())
    properties.setProperty(LaunchConfigFactory.Property.PORT, "0")
    properties.setProperty(GroovyScriptHandlerFactory.COMPILE_STATIC_PROPERTY_NAME, compileStatic.toString())
    properties.setProperty(GroovyScriptHandlerFactory.SCRIPT_PROPERTY_NAME, ratpackFile.name)
  }

  def "Absolute Path Redirect"() {
    given:
    app {
      script """
        ratpack {
          handlers {
            get {
              redirect("http://www.google.com")
            }
          }
        }
      """
    }
    when:

    def resp = get("")

    then:
    resp.statusCode == 302
    resp.getHeader("Location") == "http://www.google.com"
  }

  def "Server Root Path Redirect no public url"() {
    given:
    app {
      script """
        ratpack {
          handlers {
            get {
              redirect("/index")
            }
          }
        }
      """
    }
    when:

    def resp = get("")

    then:
    resp.statusCode == 302
    resp.getHeader("Location") == "http://${server.bindHost}:${server.bindPort}/index"
  }

  def "Server Relative Path Redirect no public url"() {
    given:
    app {
      script """
        ratpack {
          handlers {
            get("index") {
              redirect("other")
            }
          }
        }
      """
    }
    when:

    def resp = get("index")

    then:
    resp.statusCode == 302
    resp.getHeader("Location") == "http://${server.bindHost}:${server.bindPort}/other"
  }

  def "Server root path redirect with public url"() {
    given:

    def publicUrl = "http://example.com"
    properties.setProperty(LaunchConfigFactory.Property.PUBLIC_ADDRESS, publicUrl)

    app {
      script """
        ratpack {
          handlers {
            get {
              redirect("/index")
            }
          }
        }
      """
    }

    when:
    def resp = get("")

    then:
    resp.statusCode == 302
    resp.getHeader("Location") == publicUrl + "/index"

  }

  def "Server Relative Path Redirect with public url"() {
    given:
    def publicUrl = "http://example.com"
    properties.setProperty(LaunchConfigFactory.Property.PUBLIC_ADDRESS, publicUrl)
    app {
      script """
        ratpack {
          handlers {
            get("index") {
              redirect("other")
            }
          }
        }
      """
    }
    when:

    def resp = get("index")

    then:
    resp.statusCode == 302
    resp.getHeader("Location") == publicUrl + "/other"
  }

}
