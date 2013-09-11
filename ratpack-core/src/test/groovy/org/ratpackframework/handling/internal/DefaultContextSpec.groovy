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

import com.jayway.restassured.specification.RequestSpecification
import org.ratpackframework.test.internal.RatpackGroovyDslSpec

class DefaultContextSpec extends RatpackGroovyDslSpec {

  Properties properties

  @Override
  protected Properties getProperties() {
    return properties
  }

  @Override
  void configureRequest(RequestSpecification requestSpecification) {
    requestSpecification.redirects().follow(false)
  }

  def "Absolute Path Redirect"() {
    when:
    app {
      handlers {
        get {
          redirect("http://www.google.com")
        }
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.getHeader("Location") == "http://www.google.com"
  }

  def "Server Root Path Redirect no public url"() {
    when:
    app {
      handlers {
        get {
          redirect("/index")
        }
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.getHeader("Location") == "http://${server.bindHost}:${server.bindPort}/index"
  }

  def "Server Relative Path Redirect no public url"() {
    when:
    app {
      handlers {
        get("index") {
          redirect("other")
        }
      }
    }

    then:
    def resp = get("index")
    resp.statusCode == 302
    resp.getHeader("Location") == "http://${server.bindHost}:${server.bindPort}/other"
  }

  def "Server root path redirect with public url"() {
    when:
    def publicUrl = "http://example.com"
    launchConfig {
      publicAddress(new URI(publicUrl))
    }

    app {
      handlers {
        get {
          redirect("/index")
        }
      }
    }

    when:

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.getHeader("Location") == publicUrl + "/index"
  }

  def "Server Relative Path Redirect with public url"() {
    when:
    def publicUrl = "http://example.com"
    launchConfig {
      publicAddress(new URI(publicUrl))
    }

    app {
      handlers {
        get("index") {
          redirect("other")
        }
      }
    }

    then:
    def resp = get("index")
    resp.statusCode == 302
    resp.getHeader("Location") == publicUrl + "/other"
  }

}
