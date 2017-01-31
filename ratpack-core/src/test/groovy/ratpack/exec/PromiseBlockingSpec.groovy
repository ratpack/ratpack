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

package ratpack.exec

import ratpack.func.Block
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

class PromiseBlockingSpec extends Specification {

  def exec = ExecHarness.harness()

  private <T> Promise<T> async(Result<T> t) {
    Promise.async { f ->
      f.accept(t)
    }
  }

  def "can block promise"() {
    when:
    def r = exec.yield {
      Blocking.get {
        Blocking.on(async(Result.success(2)))
      }
    }

    then:
    r.valueOrThrow == 2
  }

  def "can block non async promise"() {
    when:
    def r = exec.yield {
      Blocking.get {
        Blocking.on(Promise.value(2))
      }
    }

    then:
    r.valueOrThrow == 2
  }

  def "can block failed promise"() {
    when:
    def r = exec.yield {
      Blocking.get {
        Blocking.on(async(Result.error(new RuntimeException("!"))))
      }
    }.throwable

    then:
    r instanceof RuntimeException
    r.message == "!"
  }

  def "can nest blocks"() {
    when:
    def r = exec.yield { e ->
      Blocking.get {
        Blocking.on(
          Blocking.get {
            Blocking.on(async(Result.success(2)).map { it * 2 })
          }.map {
            it * 2
          }
        )
      }
    }.valueOrThrow

    then:
    r == 8
  }

  def "cannot use block when not blocking"() {
    when:
    exec.yield {
      Blocking.on(async(Result.success(2)))
    }.valueOrThrow

    then:
    def e = thrown ExecutionException
    e.message.startsWith("Blocking.on() can only be used while blocking")
  }

  def "cannot use block when in block"() {
    when:
    exec.yield {
      Blocking.get {
        Blocking.on(
          Promise.async {
            Blocking.on(Promise.value(2))
          }
        )
      }
    }.valueOrThrow

    then:
    def e = thrown ExecutionException
    e.message.startsWith("Blocking.on() can only be used while blocking")
  }

  def "can intercept when using block"() {
    when:
    def events = []
    def latch = new CountDownLatch(4)
    def interceptor = new ExecInterceptor() {
      @Override
      void intercept(Execution execution, ExecInterceptor.ExecType execType, Block executionSegment) throws Exception {
        events << "$execType-start"
        try {
          executionSegment.execute()
        } finally {
          events << "$execType-stop"
          latch.countDown()
        }
      }
    }

    exec.yield({ it.add(interceptor) }, {
      Blocking.get {
        Blocking.on(async(Result.success(2)))
      }
    }).valueOrThrow

    then:
    latch.await()
    events == [
      "COMPUTE-start",
      "COMPUTE-stop",
      "BLOCKING-start",
      "COMPUTE-start",
      "COMPUTE-stop",
      "BLOCKING-stop",
      "COMPUTE-start",
      "COMPUTE-stop"
    ]
  }

}
