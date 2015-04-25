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

package ratpack.session

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.session.store.MapSessionsModule
import ratpack.session.store.SessionStorage
import ratpack.session.store.SessionStore
import ratpack.test.internal.RatpackGroovyDslSpec

class SessionSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new SessionModule()
    modules << new MapSessionsModule(10, 5)
    bindings {
      bindInstance ServerErrorHandler, new DefaultDevelopmentErrorHandler()
    }
  }

  def "can use session"() {
    when:
    handlers {
      get(":v") { Session session ->
        render session.id
      }
    }

    then:
    getText("a") == getText("b")
  }

  def "can store session vars"() {
    when:
    handlers {
      get("") { SessionStorage storage ->
//        render storage.value.toString()
        storage.get("value",String).then({ Optional<String> display ->
          if(display.isPresent()){
            display.ifPresent({String show ->
              render show
            })
          } else {
            render "Missing"
          }
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value",pathTokens.value).then({
          storage.get("value",String).then({
            render it
          })
        })

      }
    }

    and:
    getText("set/foo") == "foo"

    then:
    getText() == "foo"
  }

  def "can invalidate session vars"() {
    when:
    handlers {
      get("") { SessionStorage storage ->
        storage.get("value",String).then({
         render it.orElse("null")
        })
      }
      get("set/:value") { SessionStorage storage ->
        storage.set("value",pathTokens.value).then({

          storage.get("value",String).then({
            render it.orElse("null")
          })
        })


      }
      get("invalidate") { Session session ->
        session.terminate()
        response.send()
      }
      get("size") { SessionStore store ->
        render store.size().toString()
      }
    }

    and:
    getText("set/foo")

    then:
    getText() == "foo"
    getText("size") == "1"

    when:
    getText("invalidate")

    then:
    getText() == "null"
    getText("size") == "1"
  }

  def "sessions are created on demand"() {
    when:
    handlers {
      get { SessionStore store ->
        render store.size().toString()
      }
      get("store") { SessionStorage storage ->
        render "ok"
      }
    }

    then:
    getText() == "0"
    getText("store") == "ok"
    getText() == "1"
  }

  def "session cookies are only set when needed"() {
    when:
    handlers {
      get("foo") {
        response.send("foo")
      }
      get("bar") { SessionStorage store ->
        response.send("bar")
      }
    }

    then:
    get("foo").headers.get("Set-Cookie") == null
    get("bar").headers.get("Set-Cookie").contains('JSESSIONID')

    // null because the session id is already set
    !get("bar").headers.get("Set-Cookie")?.contains('JSESSIONID')
  }
}
