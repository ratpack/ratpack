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

import com.google.common.net.HostAndPort
import io.netty.util.CharsetUtil
import ratpack.error.ServerErrorHandler
import ratpack.func.Action
import ratpack.groovy.internal.ClosureUtil
import ratpack.groovy.test.GroovyUnitTest
import ratpack.groovy.test.handling.GroovyRequestFixture
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.handling.RequestOutcome
import ratpack.registry.Registries
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static ratpack.groovy.Groovy.groovyHandler
import static ratpack.handling.Handlers.chain

class HandlerUnitTestingSpec extends Specification {

  GroovyRequestFixture fixture = GroovyUnitTest.requestFixture()

  @Delegate
  HandlingResult result

  // Declare this method due to https://jira.codehaus.org/browse/GROOVY-7118
  public <T extends Throwable> T exception(Class<T> clazz) {
    result.exception(clazz)
  }

  void handle(@DelegatesTo(Context) Closure handler) {
    result = fixture.handle(groovyHandler(handler))
  }

  void fixture(@DelegatesTo(GroovyRequestFixture) Closure config) {
    ClosureUtil.configureDelegateFirst(fixture, config)
  }

  void invoke(Handler handler) {
    result = fixture.handle(handler)
  }

  def "can test handler that just calls next"() {
    when:
    handle { next() }

    then:
    bodyText == null
    bodyBytes == null
    calledNext
    !sentResponse
    sentFile == null
  }

  def "can test handler that sends string"() {
    when:
    handle { response.send "foo" }

    then:
    bodyText == "foo"
    bodyBytes == "foo".getBytes(CharsetUtil.UTF_8)
    !calledNext
    sentResponse
    sentFile == null
    headers.get("content-type") == "text/plain;charset=UTF-8"
  }

  def "can test handler that sends bytes"() {
    when:
    handle { response.send "foo".getBytes(CharsetUtil.UTF_8) }

    then:
    bodyText == "foo"
    bodyBytes == "foo".getBytes(CharsetUtil.UTF_8)
    !calledNext
    sentResponse
    headers.get("content-type") == "application/octet-stream"
    sentFile == null
  }

  def "can test handler that sends file"() {
    when:
    handle { response.contentType("text/plain").sendFile new File("foo").toPath() }

    then:
    bodyText == null
    bodyBytes == null
    !calledNext
    !sentResponse
    sentFile == new File("foo").toPath()
    headers.get("content-type") == "text/plain"
  }

  def "can test handler that sends file calls onClose"() {
    given:
    def onCloseCalled = false
    def onCloseCalledWrapper = new BlockingVariable<Boolean>(1)

    when:
    handle {
      onClose { onCloseCalled = true; onCloseCalledWrapper.set(onCloseCalled) }
      response.contentType("text/plain").sendFile(new File("foo").toPath())
    }

    then:
    bodyText == null
    bodyBytes == null
    !calledNext
    !sentResponse
    sentFile == new File("foo").toPath()
    headers.get("content-type") == "text/plain"
    onCloseCalledWrapper.get()
  }

  def "can register things"() {
    given:
    fixture.registry.add "foo"

    when:
    handle { response.send get(String) }

    then:
    bodyText == "foo"
  }

  def "can test async handlers"() {
    given:
    fixture.timeout 3

    when:
    handle { Thread.start { sleep 1000; next() } }

    then:
    calledNext
  }

  def "will throw if handler takes too long"() {
    given:
    fixture.timeout 1

    when:
    handle { Thread.start { sleep 2000; next() } }

    then:
    thrown HandlerTimeoutException
  }

  def "will throw if no exception thrown"() {
    when:
    handle {
      next()
    }
    exception(Exception)

    then:
    thrown(HandlerExceptionNotThrownException)
  }

  def "can set uri"() {
    given:
    fixture.uri "foo"

    when:
    handle { response.send request.uri }

    then:
    bodyText == "/foo"
  }

  def "can set request method"() {
    given:
    fixture.method "PUT"

    when:
    handle { response.send request.method.name }

    then:
    bodyText == "PUT"
  }

  def "can set request headers"() {
    given:
    fixture.header "X-Requested-With", "Spock"

    when:
    handle { response.send request.headers.get("X-Requested-With") }

    then:
    bodyText == "Spock"
  }

  def "can set response headers"() {
    given:
    fixture.responseHeader "Via", "Ratpack"

    when:
    handle { response.send response.headers.get("Via") }

    then:
    bodyText == "Ratpack"
  }

  def "can test handler with onClose event registered"() {
    def latch = new CountDownLatch(2)

    when:
    handle {
      onClose(new Action<RequestOutcome>() {
        @Override
        void execute(RequestOutcome requestOutcome) throws Exception {
          latch.countDown()
        }
      })

      onClose(new Action<RequestOutcome>() {
        @Override
        void execute(RequestOutcome requestOutcome) throws Exception {
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
    fixture.body(*arguments)

    when:
    handle {
      response.headers.set "X-Request-Content-Length", request.headers.get("Content-Length")
      response.headers.set "X-Request-Content-Type", request.headers.get("Content-Type")
      response.send request.body.bytes
    }

    then:
    bodyBytes == responseBytes
    headers.get("X-Request-Content-Type") == responseContentType
    headers.get("X-Request-Content-Length") == "$responseBytes.length"

    where:
    arguments                             | responseContentType        | responseBytes
    [[0, 1, 2, 4] as byte[], "image/png"] | "image/png"                | [0, 1, 2, 4] as byte[]
    ["foo", "text/plain"]                 | "text/plain" | "foo".bytes
  }

  def "captures errors"() {
    when:
    handle {
      error(new RuntimeException("!"))
    }

    then:
    RuntimeException e = exception(RuntimeException)
    e.message == "!"
  }

  def "throws error if accessing results when exception thrown"() {
    when:
    handle {
      error(new RuntimeException("!"))
    }
    bodyText

    then:
    UnexpectedHandlerException e = thrown(UnexpectedHandlerException)
    assert e.cause.message == '!'
  }

  def "captures client errors"() {
    when:
    handle {
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
    fixture {
      registry {
        add("foo")
        add("bar")
      }
    }

    when:
    handle {
      render getAll(String).join(",")
    }

    then:
    rendered(String) == "foo,bar"
  }

  def "can easily add path tokens for unit tests"() {
    given:
    fixture {
      pathBinding a: "1", b: "2"
    }

    when:
    handle {
      render pathTokens.toString()
    }

    then:
    rendered(String) == [a: "1", b: "2"].toString()
  }

  @Unroll
  def "can access things inserted into registry"() {
    when:
    handle {
      insert(Registries.just("foo"), groovyHandler {
        blocking {

        } then {
          context.insert(Registries.just("bar"), groovyHandler {
            context.request.add(Number, 4)
            function(context)
          })
        }
      })
    }

    then:
    registry.getAll(String).toList() == ["bar", "foo"]
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
      void error(Context context, Throwable throwable) throws Exception {
        context.render(throwable.message)
      }
    }

    when:
    fixture {
      registry {
        add(ServerErrorHandler, errorHandler)
      }
    }

    and:
    handle {
      throw thrown
    }

    then:
    rendered(String) == "!"
  }

  def "can redirect"() {
    when:
    handle {
      redirect "/foo"
    }

    then:
    result.sentResponse
    result.status.code == 302
    result.headers.location == "http://localhost:5050/foo"
  }

  @Unroll
  def "can get remote host and port"() {
    given:
    fixture.remoteAddress(HostAndPort.fromParts(host, port))

    when:
    handle {
      response.send request.remoteAddress.toString()
    }

    then:
    result.bodyText == "${host}:${port}"

    where:
    host          | port
    'localhost'   | 8080
    'ratpack.io'  | 45678
  }

}
