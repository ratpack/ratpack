/*
 * Copyright 2015 the original author or authors.
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

package ratpack.http

import io.netty.buffer.Unpooled
import io.netty.handler.codec.PrematureChannelClosureException
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.func.Block
import ratpack.groovy.handling.GroovyContext
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch

class ResponseBodyReleaseSpec extends RatpackGroovyDslSpec {

  def buffer = Unpooled.buffer()
  def requestExecutionCompleted = new CountDownLatch(1)

  void connectAndTerminate() {
    SocketTimeoutException timeoutException
    HttpURLConnection connection = address.toURL().openConnection()
    try {
      connection.readTimeout = 1
      connection.inputStream
    } catch (SocketTimeoutException e) {
      timeoutException = e
    } finally {
      assert timeoutException
      connection.disconnect()
    }
  }

  void executionCompletionReportingHandler(@DelegatesTo(value = GroovyContext, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    handlers {
      all({
        Execution.current().onComplete { requestExecutionCompleted.countDown() }
      } << handler)
    }
  }

  def "body is released if the client closes the connection before headers are sent"() {
    given:
    def connectionTerminated = new CountDownLatch(1)

    and:
    executionCompletionReportingHandler {
      Blocking.op(Block.noop()) //jump off thread for the chanel close event to be processed
        .then {
        connectionTerminated.await()
        response.send(buffer)
      }
    }

    when:
    connectAndTerminate()

    and:
    connectionTerminated.countDown()
    requestExecutionCompleted.await()

    then:
    buffer.refCnt() == 0
  }

  def "body is released if the response is sent more than once"() {
    given:
    def secondResponseBuffer = Unpooled.buffer()

    and:
    executionCompletionReportingHandler {
      response.send(buffer)
      response.send(secondResponseBuffer)
    }

    when:
    get()

    and:
    requestExecutionCompleted.await()

    then:
    secondResponseBuffer.refCnt() == 0
  }

  def "body is released if there is an error while sending headers"() {
    given:
    executionCompletionReportingHandler {
      response.status(new TransmissionErrorCausingStatus())
      response.send(buffer)
    }

    when:
    get()

    then:
    thrown(PrematureChannelClosureException)

    and:
    requestExecutionCompleted.await()

    then:
    buffer.refCnt() == 0
  }

  private static class TransmissionErrorCausingStatus implements Status {
    @Override
    int getCode() {
      return 200
    }

    @Override
    String getMessage() {
      return "OK"
    }

    @Override
    HttpResponseStatus getNettyStatus() {
      return new HttpResponseStatus(code, message) {
        int code() {
          throw new UnsupportedOperationException()
        }
      }
    }
  }
}
