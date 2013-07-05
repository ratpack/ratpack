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

package org.ratpackframework.http.internal

import com.jayway.restassured.RestAssured
import com.jayway.restassured.specification.RequestSpecification
import org.ratpackframework.test.groovy.RatpackGroovyScriptAppSpec

class DefaultResponseRedirectSpec extends RatpackGroovyScriptAppSpec {

  @Override
  RequestSpecification createRequest() {
    //We are testing redirects so its best not to follow them
    RestAssured.with().urlEncodingEnabled(false).redirects().follow(false)
  }

  def "Absolute Path Redirect"() {
    given:
    app {
      script """
        ratpack {
          handlers {
            get {
              response.redirect("http://www.google.com")
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
              response.redirect("/index")
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
              response.redirect("other")
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

}
