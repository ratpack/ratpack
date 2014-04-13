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

package ratpack.test.handling

import io.netty.util.CharsetUtil
import ratpack.error.ServerErrorHandler
import ratpack.func.Action
import ratpack.groovy.internal.ClosureUtil
import ratpack.groovy.test.GroovyUnitTest
import ratpack.groovy.test.handling.GroovyInvocationBuilder
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.handling.RequestOutcome
import ratpack.registry.Registries
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static ratpack.groovy.Groovy.groovyHandler
import static ratpack.handling.Handlers.chain

class InvocationBuilderSpec extends Specification {

  @Subject
  GroovyInvocationBuilder builder = GroovyUnitTest.invocationBuilder()

  @Delegate
  Invocation invocation

  void invoke(@DelegatesTo(Context) Closure handler) {
    invocation = builder.invoke(groovyHandler(handler))
  }

  void builder(@DelegatesTo(GroovyInvocationBuilder) Closure config) {
    ClosureUtil.configureDelegateFirst(builder, config)
  }

  void invoke(Handler handler) {
    invocation = builder.invoke(handler)
  }

  def "can test handler that just calls next"() {
    when:
    invoke { next() }

    then:
    bodyText == null
    bodyBytes == null
    calledNext
    !sentResponse
    exception == null
    sentFile == null
  }

  def "can test handler that sends string"() {
    when:
    invoke { response.send "foo" }

    then:
    bodyText == "foo"
    bodyBytes == "foo".getBytes(CharsetUtil.UTF_8)
    !calledNext
    sentResponse
    exception == null
    sentFile == null
    headers.get("content-type") == "text/plain;charset=UTF-8"
  }

  def "can test handler that sends bytes"() {
    when:
    invoke { response.send "foo".getBytes(CharsetUtil.UTF_8) }

    then:
    bodyText == "foo"
    bodyBytes == "foo".getBytes(CharsetUtil.UTF_8)
    !calledNext
    sentResponse
    exception == null
    headers.get("content-type") == "application/octet-stream"
    sentFile == null
  }

  def "can test handler that sends file"() {
    when:
    invoke { response.contentType("text/plain").sendFile context, new File("foo").toPath() }

    then:
    bodyText == null
    bodyBytes == null
    !calledNext
    !sentResponse
    exception == null
    sentFile == new File("foo").toPath()
    headers.get("content-type") == "text/plain;charset=UTF-8"
  }

  def "can register things"() {
    given:
    builder.register "foo"

    when:
    invoke { response.send get(String) }

    then:
    bodyText == "foo"
  }

  def "can test async handlers"() {
    given:
    builder.timeout 3

    when:
    invoke { Thread.start { sleep 1000; next() } }

    then:
    calledNext
  }

  def "will throw if handler takes too long"() {
    given:
    builder.timeout 1

    when:
    invoke { Thread.start { sleep 2000; next() } }

    then:
    thrown InvocationTimeoutException
  }

  def "can set uri"() {
    given:
    builder.uri "foo"

    when:
    invoke { response.send request.uri }

    then:
    bodyText == "/foo"
  }

  def "can set request method"() {
    given:
    builder.method "PUT"

    when:
    invoke { response.send request.method.name }

    then:
    bodyText == "PUT"
  }

  def "can set request headers"() {
    given:
    builder.header "X-Requested-With", "Spock"

    when:
    invoke { response.send request.headers.get("X-Requested-With") }

    then:
    bodyText == "Spock"
  }

  def "can set response headers"() {
    given:
    builder.responseHeader "Via", "Ratpack"

    when:
    invoke { response.send response.headers.get("Via") }

    then:
    bodyText == "Ratpack"
  }

  def "can test handler with onClose event registered"() {
    def latch = new CountDownLatch(2)

    when:
    invoke {
      onClose(new Action<RequestOutcome>() {
        @Override
        void execute(RequestOutcome requestOutcome) {
          latch.countDown()
        }
      })

      onClose(new Action<RequestOutcome>() {
        @Override
        void execute(RequestOutcome requestOutcome) {
          latch.countDown()
        }
      })

      response.send "foo"
    }

    then:
    latch.await(2, TimeUnit.SECONDS)
    latch.count == 0
    bodyText == "foo"
    sentResponse
  }

  @Unroll
  def "can set request body"() {
    //noinspection GroovyAssignabilityCheck
    given:
    builder.body(*arguments)

    when:
    invoke {
      response.headers.set "X-Request-Content-Length", request.headers.get("Content-Length")
      response.headers.set "X-Request-Content-Type", request.headers.get("Content-Type")
      response.send request.body.bytes
    }

    then:
    bodyBytes == responseBytes
    headers.get("X-Request-Content-Type") == responseContentType
    headers.get("X-Request-Content-Length") == "$responseBytes.length"

    where:
    arguments | responseContentType | responseBytes
    [[0, 1, 2, 4] as byte[], "image/png"] | "image/png"                | [0, 1, 2, 4] as byte[]
    ["foo", "text/plain"]                 | "text/plain;charset=UTF-8" | "foo".bytes
  }

  def "captures errors"() {
    when:
    invoke {
      error(new RuntimeException("!"))
    }

    then:
    exception instanceof RuntimeException
    exception.message == "!"
  }

  def "captures client errors"() {
    when:
    invoke {
      clientError 404
    }

    then:
    clientError == 404
  }

  def "rendered downstream objects are captured"() {
    when:
    invoke chain({ it.next() } as Handler, { it.render("foo") } as Handler)

    then:
    rendered(String) == "foo"
  }

  def "can register object via builder"() {
    given:
    builder {
      registry {
        add("foo")
        add("bar")
      }
    }

    when:
    invoke {
      render getAll(String).join(",")
    }

    then:
    rendered(String) == "foo,bar"
  }

  def "can easily add path tokens for unit tests"() {
    given:
    builder {
      pathBinding a: "1", b: "2"
    }

    when:
    invoke {
      render pathTokens.toString()
    }

    then:
    rendered(String) == [a: "1", b: "2"].toString()
  }

  @Unroll
  def "can access things inserted into registry"() {
    when:
    invoke {
      insert(Registries.registry("foo"), groovyHandler {
        background {

        } then {
          context.insert(Registries.registry("bar"), groovyHandler {
            context.request.register(Number, 4)
            function(context)
          })
        }
      })
    }

    then:
    registry.getAll(String) == ["bar", "foo"]
    requestRegistry.get(Number) == 4

    where:
    function << [
      { Context it -> it.render "ok" },
      { Context it -> it.response.send() },
      { Context it -> it.error(new Exception()) },
      { Context it -> it.clientError(404) },
    ]
  }

  def "custom error handler receives errors"() {
    given:
    def thrown = new RuntimeException("!")
    def errorHandler = new ServerErrorHandler() {
      @Override
      void error(Context context, Exception exception) throws Exception {
        context.render(exception.message)
      }
    }

    when:
    builder {
      registry {
        add(ServerErrorHandler, errorHandler)
      }
    }

    and:
    invoke {
      throw thrown
    }

    then:
    rendered(String) == "!"
  }

}
