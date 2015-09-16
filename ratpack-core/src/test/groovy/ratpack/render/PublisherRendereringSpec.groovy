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
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec

class PublisherRendereringSpec extends RatpackGroovyDslSpec {

  def "can render publisher"() {
    when:
    handlers {
      get {
        context.render(Streams.publish(["foo"]))
      }
    }

    then:
    text == "foo"
  }

  def "can render publisher bound to execution"() {
    when:
    handlers {
      get {
        context.render(Streams.publish(["foo"]))
      }
    }

    then:
    text == "foo"
  }

  def "fails to render publisher with more than one elements"() {
    when:
    bindings {
      bindInstance ServerErrorHandler, { ctx, e -> ctx.render e.message } as ServerErrorHandler
    }
    handlers {
      get {
        context.render(Streams.publish(["foo", "bar"]))
      }
    }

    then:
    text == "Cannot convert stream of more than 1 item to a Promise"
  }

  def "can render failed publisher"() {
    when:
    bindings {
      bindInstance ServerErrorHandler, { ctx, e -> ctx.render e.message } as ServerErrorHandler
    }
    handlers {
      get {
        context.render(Streams.yield { throw new Exception("foo") })
      }
    }

    then:
    text == "foo"
  }

}
