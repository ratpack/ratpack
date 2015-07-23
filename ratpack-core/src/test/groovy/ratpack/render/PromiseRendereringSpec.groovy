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

package ratpack.render

import ratpack.error.ServerErrorHandler
import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec

class PromiseRendereringSpec extends RatpackGroovyDslSpec {

  def "can render promise"() {
    when:
    handlers {
      get {
        context.render(Blocking.get { "foo" })
      }
    }

    then:
    text == "foo"
  }

  def "can render success promise"() {
    when:
    handlers {
      get {
        context.render(Blocking.get { "foo" }.onError { throw new Error() })
      }
    }

    then:
    text == "foo"
  }

  def "can render failed promise"() {
    when:
    bindings {
      bindInstance ServerErrorHandler, { ctx, e -> ctx.render e.message } as ServerErrorHandler
    }
    handlers {
      get {
        context.render(Blocking.get { throw new Exception("foo") })
      }
    }

    then:
    text == "foo"
  }

  def "can render failed success promise"() {
    when:
    handlers {
      get {
        context.render(Blocking.get { throw new Exception("foo") }.onError { render it.message })
      }
    }

    then:
    text == "foo"
  }
}
