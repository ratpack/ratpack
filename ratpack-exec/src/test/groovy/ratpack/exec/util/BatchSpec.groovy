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

package ratpack.exec.util

import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class BatchSpec extends Specification {

  @AutoCleanup
  ExecHarness exec = ExecHarness.harness()

  def "parallel yieldAll all success"() {
    given:
    def promises = (1..9).collect { i ->
      Blocking.get { "Promise $i" }
    }

    when:
    def result = exec.yieldSingle { e ->
      ParallelBatch.of(promises).yieldAll()
    }.valueOrThrow

    then:
    result.collect { it.valueOrThrow } == (1..9).collect { "Promise ${it}".toString() }
  }

  def "parallel yield all success"() {
    given:
    def promises = (1..9).collect { i -> Blocking.get { "Promise $i" } }

    when:
    def result = exec.yieldSingle { e ->
      ParallelBatch.of(promises).yield()
    }.valueOrThrow

    then:
    result == (1..9).collect { "Promise ${it}".toString() }
  }

  def "yield failure parallel"() {
    given:
    def handles = new ConcurrentLinkedQueue()
    def counter = new AtomicInteger(10)
    def promises = (0..9).collect { i ->
      Blocking.get {
        def v = Execution.current().get(Integer)
        if (v > 5) {
          throw new RuntimeException("!")
        } else {
          "Promise $v"
        }
      }.defer {
        handles << it
        if (counter.decrementAndGet() == 0) {
          handles.each { it.run() }
        }
      }
    }

    when:
    def i = new AtomicInteger()
    def t = exec.yieldSingle { e ->
      ParallelBatch.of(promises).execInit {
        it.add(Integer, i.getAndIncrement())
      }.yield()
    }.throwable

    then:
    t instanceof RuntimeException
    t.suppressed.length == 3
  }

  def "yield all failure parallel"() {
    given:
    def promises = (0..9).collect { i ->
      Blocking.get {
        def v = Execution.current().get(Integer)
        if (v > 5) {
          throw new RuntimeException("!")
        } else {
          "Promise $v"
        }
      }
    }

    when:
    def i = new AtomicInteger()
    def t = exec.yieldSingle { e ->
      ParallelBatch.of(promises).execInit {
        it.add(Integer, i.getAndIncrement())
      }.yieldAll()
    }.valueOrThrow

    then:
    t.size() == 10
    t[0..5].each { it.success }
    t[6..9].each { it.error }
  }

  def "consume parallel publisher"() {
    when:
    def promises = (1..9).collect { i -> Blocking.get { "Promise $i" } }
    def p = ParallelBatch.of(promises).publisher()

    then:
    def l = exec.yield { p.toList() }.valueOrThrow
    l.sort(false) == (1..9).collect { "Promise $it" }
  }

  def "consume publisher"() {
    when:
    def promises = (1..9).collect { i -> Blocking.get { "Promise $i" } }
    def p = SerialBatch.of(promises).publisher()

    then:
    def l = exec.yield { p.toList() }.valueOrThrow
    l == (1..9).collect { "Promise $it" }
  }

  def "consume parallel publisher error"() {
    when:
    def promises = (1..9).collect { i -> i < 2 ? Promise.error(new RuntimeException("1")) : Promise.value(i) }
    def p = ParallelBatch.of(promises).publisher()
    exec.yield { p.toList() }.valueOrThrow

    then:
    thrown RuntimeException
  }

  def "parallel batch may be empty"() {
    expect:
    def batch = ParallelBatch.of(Collections.emptyList())
    exec.yield { batch.yield() }.value.empty
    exec.yield { batch.yieldAll() }.value.empty
    exec.execute(batch.forEach {})
    exec.yield { batch.publisher().toList() }.value.empty
  }

  def "serial batch may be empty"() {
    expect:
    def batch = SerialBatch.of(Collections.emptyList())
    exec.yield { batch.yield() }.value.empty
    exec.yield { batch.yieldAll() }.value.empty
    exec.execute(batch.forEach {})
    exec.yield { batch.publisher().toList() }.value.empty
  }

  def "parallel batch can have promises with error handling"() {
    when:
    def r = exec.yield {
      ParallelBatch.of(Promise.error(new RuntimeException("1")).onError {}, Promise.value(1)).yieldAll()
    }.value

    then:
    r.size() == 2
    r[0].value == null
    r[1].value == 1
  }
}
