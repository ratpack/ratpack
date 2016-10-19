/*
 * Copyright 2016 the original author or authors.
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

package ratpack.session

import com.google.inject.AbstractModule
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

class SessionTerminationSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new SessionModule()
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(SimpleErrorHandler)
      }
    }
  }

  def "terminating session assigns new session id"() {
    when:
    handlers {
      get("read") { Session session ->
        render session.id
      }
      get("terminate") { Session session ->
        session.terminate().then {
          render session.id
        }
      }
    }
    def beforeTerminate = getText("read")
    def afterTerminate = getText("terminate")

    then:
    beforeTerminate
    afterTerminate
    beforeTerminate != "null"
    afterTerminate != "null"
    beforeTerminate != afterTerminate
  }

  def "terminating session starts a new session"() {
    when:
    handlers {
      get("set/:value") { Session session ->
        session.set("value", pathTokens.value).then {
          render pathTokens.value
        }
      }
      get("get") { Session session ->
        render session.require("value")
      }
      get("terminate/:value") { Session session ->
        session.terminate().then {
          session.set("value", pathTokens.value).then {
            render pathTokens.value
          }
        }
      }
    }

    then:
    get("set/foo")
    getText("get") == "foo"
    get("terminate/bar")
    getText("get") == "bar"
  }
}
