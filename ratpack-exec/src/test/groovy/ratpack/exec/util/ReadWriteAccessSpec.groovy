/*
 * Copyright 2017 the original author or authors.
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

package ratpack.exec.util

import ratpack.exec.BaseExecutionSpec
import ratpack.exec.Execution
import ratpack.exec.Promise

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ReadWriteAccessSpec extends BaseExecutionSpec {

  def access = ReadWriteAccess.create()

  @SuppressWarnings("ChangeToOperator")
  def "serializes reads and writes"() {
    when:
    def ops = 100_000
    def readCounter = new AtomicInteger()
    def writeCounter = new AtomicInteger()
    def ratio = 10

    def p = []
    ops.times { n ->
      def write = n % ratio == 0
      p << Promise.sync {
        if (write) {
          assert readCounter.get() == 0
          assert writeCounter.incrementAndGet() == 1
        } else {
          assert writeCounter.get() == 0
          readCounter.incrementAndGet()
        }
        n
      }.
        next {
          if (write) {
            assert readCounter.get() == 0
            assert writeCounter.decrementAndGet() == 0
          } else {
            assert writeCounter.get() == 0
            readCounter.decrementAndGet()
          }
        }.
        apply(write ? access.&write : access.&read)
    }

    def values = execHarness.yield {
      ParallelBatch.of(p).yield()
    }.valueOrThrow

    then:
    values.sort(false).size() == ops
  }

  def "relinquishes lock on error"() {
    when:
    access.write(Promise.error(new RuntimeException("!"))).yield()

    then:
    thrown RuntimeException

    when:
    access.read(Promise.error(new RuntimeException("!"))).yield()

    then:
    thrown RuntimeException

    when:
    access.write(Promise.error(new RuntimeException("!"))).yield()

    then:
    thrown RuntimeException
  }

  def "all executions complete with heavy read contention"() {
    when:
    def n = 1_000_000
    def l = new CountDownLatch(n)
    def i = new AtomicInteger()

    n.times {
      execHarness.controller.fork().start {
        def d = new AtomicBoolean()
        Execution.current().onComplete {
          if (!d.get()) {
            i.incrementAndGet()
          }
          l.countDown()
        }
        access.read(Promise.value(1)).then { d.set(true) }
      }
    }

    then:
    l.await()
    i.get() == 0
  }
}
