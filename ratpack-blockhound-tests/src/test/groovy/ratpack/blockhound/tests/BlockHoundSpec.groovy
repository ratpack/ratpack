/*
 * Copyright 2022 the original author or authors.
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

package ratpack.blockhound.tests

import ratpack.exec.Operation
import ratpack.exec.Promise
import ratpack.test.RatpackBlockHound
import ratpack.test.exec.ExecHarness
import reactor.blockhound.BlockingOperationError
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.CountDownLatch

class BlockHoundSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()

  def setupSpec() {
   RatpackBlockHound.install()
  }

  def "fail blocking calls in Operation"() {
    when:
    harness.execute { e ->
      Operation.of {
        Thread.sleep(50)
      }
    }

    then:
    def ex = thrown(BlockingOperationError)
    ex.message.contains("Blocking call")
  }

  def "fail blocking calls in Promise"() {
    when:
    harness.yield { e ->
      Promise.sync {
        Thread.sleep(50)
        "ok"
      }
    }.valueOrThrow

    then:
    def ex = thrown(BlockingOperationError)
    ex.message.contains("Blocking call")

    when:
    harness.yield { e ->
      Promise.async { d ->
        Thread.sleep(50)
        d.success("ok")
      }
    }.valueOrThrow

    then:
    ex = thrown(BlockingOperationError)
    ex.message.contains("Blocking call")

    when:
    harness.yield { e ->
      Promise.sync {
        "ok"
      }.map {v ->
        Thread.sleep(50)
        v.toUpperCase()
      }
    }.valueOrThrow

    then:
    ex = thrown(BlockingOperationError)
    ex.message.contains("Blocking call")

    when:
    harness.yield { e ->
      Promise.sync {
        "ok"
      }.flatMap {v ->
        Thread.sleep(50)
        Promise.sync {
          v.toUpperCase()
        }
      }
    }.valueOrThrow

    then:
    ex = thrown(BlockingOperationError)
    ex.message.contains("Blocking call")
  }

  def "allows blocking calls in Operation methods"() {
    given:
    CountDownLatch latch = new CountDownLatch(1)
    BlockingVariable<String> value = new BlockingVariable<>()

    when:
    harness.execute { e ->
      Operation.of {
        latch.countDown()
      }.blockingNext {
        Thread.sleep(50)
        value.set("ok")
      }
    }

    then:
    latch.await()
    value.get() == "ok"
  }

  def "allows blocking calls in Promise methods"() {
    given:
    BlockingVariable<String> value = new BlockingVariable<>()

    when:
    def result = harness.yield { e ->
      Promise.sync {
        "ok"
      }.blockingMap { v ->
        Thread.sleep(50)
        v.toUpperCase()
      }
    }.valueOrThrow

    then:
    noExceptionThrown()
    result == "OK"

    when:
    harness.yield { e->
      Promise.sync{
        "ok"
      }.blockingOp { v ->
        Thread.sleep(50)
        value.set(v)
      }
    }.valueOrThrow

    then:
    noExceptionThrown()
    value.get() == "ok"
  }
}
