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

package ratpack.exec

import ratpack.exec.util.retry.AttemptRetryPolicy
import ratpack.exec.util.retry.DurationRetryPolicy
import ratpack.exec.util.retry.FixedDelay
import ratpack.exec.util.retry.IndexedDelay
import ratpack.test.internal.time.FixedWindableClock

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

class PromiseRetrySpec extends BaseExecutionSpec {

  def clock = new FixedWindableClock(Instant.now(), ZoneOffset.UTC)

  def attemptFixedRetryStrategy = AttemptRetryPolicy.of { it
    .delay(FixedDelay.of(Duration.ofMillis(5)))
    .maxAttempts(3)
  }

  def attemptIndexedRetryStrategy = AttemptRetryPolicy.of { it
    .delay(IndexedDelay.of { i -> Duration.ofMillis(5).multipliedBy(i) })
    .maxAttempts(3)
  }

  def durationRetryStrategy = DurationRetryPolicy.of { it
    .delay(FixedDelay.of(Duration.ofMillis(5)))
    .maxDuration(Duration.ofSeconds(10))
    .clock(clock)
  }

  def predicate = { t -> t instanceof IOException }

  def "can retry failed promise and succeed (deprecated method)"() {
    when:
    def i = new AtomicInteger()

    exec {
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw new IllegalStateException() })
        .retry(3, { n, e -> events << "retry$n"; Promise.value(Duration.ofMillis(5 * n)) })
        .then(events.&add)
    }

    then:
    events == ["retry1", "retry2", 3, "complete"]
  }

  def "can retry failed promise and fail (deprecated method)"() {
    when:
    def e = new RuntimeException("!")

    exec({
      Promise.error(e)
        .retry(3, { n, ex -> events << "retry$n"; Promise.value(Duration.ofMillis(5 * n)) })
        .then { events << "then" }
    }, events.&add)

    then:
    events == ["retry1", "retry2", "retry3", e, "complete"]
  }

  def "can retry failed promise with fixed delay and succeed (deprecated method)"() {
    when:
    def i = new AtomicInteger()

    exec {
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw new IllegalStateException() })
        .retry(3, Duration.ofMillis(5), { n, e -> events << "retry$n" })
        .then(events.&add)
    }

    then:
    events == ["retry1", "retry2", 3, "complete"]
  }

  def "can retry failed promise with fixed delay and fail (deprecated method)"() {
    when:
    def e = new RuntimeException("!")

    exec({
      Promise.error(e)
        .retry(3, Duration.ofMillis(5), { n, ex -> events << "retry$n" })
        .then { events << "then" }
    }, events.&add)

    then:
    events == ["retry1", "retry2", "retry3", e, "complete"]
  }

  def "promise retry action is an execution segment (deprecated method)"() {
    when:
    def e = new RuntimeException("!")

    exec({
      Promise.error(e)
        .retry(3, Duration.ofMillis(5), { n, ex -> Operation.of { events << "retry$n" }.then() })
        .then { events << "then" }
    }, events.&add)

    then:
    events == ["retry1", "retry2", "retry3", e, "complete"]
  }

  def "can retry failed promise until a number of attempts and succeed"() {
    when:
    def i = new AtomicInteger()

    exec {
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw new IllegalStateException() })
        .retry(attemptFixedRetryStrategy, { n, e -> events << "retry$n" })
        .then(events.&add)
    }

    then:
    events == ["retry1", "retry2", 3, "complete"]
  }

  def "can retry with an indexed delay failed promise until a number of attempts and succeed"() {
    when:
    def i = new AtomicInteger()

    exec {
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw new IllegalStateException() })
        .retry(attemptIndexedRetryStrategy, { n, e -> events << "retry$n" })
        .then(events.&add)
    }

    then:
    events == ["retry1", "retry2", 3, "complete"]
  }

  def "can retry failed promise until a number of attempts and fail"() {
    when:
    def e = new RuntimeException("!")

    exec({
      Promise.error(e)
        .retry(attemptFixedRetryStrategy, { n, ex -> events << "retry$n" })
        .then { events << "then" }
    }, events.&add)

    then:
    events == ["retry1", "retry2", "retry3", e, "complete"]
  }

  def "can retry failed promise until a duration and succeed"() {
    when:
    def i = new AtomicInteger()

    exec {
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw new IllegalStateException() })
        .retry(durationRetryStrategy, { n, e -> events << "retry$n" })
        .then(events.&add)
    }

    then:
    events == ["retry1", "retry2", 3, "complete"]
  }

  def "can retry failed promise until a duration and fail"() {
    when:
    def e = new RuntimeException("!")

    exec({
      Promise.error(e)
        .retry(durationRetryStrategy, { n, ex ->
        events << "retry$n"
        if (n == 3) {
          clock.windClock(Duration.ofMinutes(5))
        }
      })
        .then { events << "then" }
    }, events.&add)

    then:
    events == ["retry1", "retry2", "retry3", e, "complete"]
  }

  def "can retry with an indexed delay failed promise if predicate matches until a number of attempts and succeed"() {
    when:
    def i = new AtomicInteger()

    exec {
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw new IOException() })
        .retryIf(predicate, attemptIndexedRetryStrategy, { n, e -> events << "retry$n" })
        .then(events.&add)
    }

    then:
    events == ["retry1", "retry2", 3, "complete"]
  }

  def "can retry failed promise if predicate matches until a number of attempts"() {
    when:
    def e = new IOException("!")

    exec({
      Promise.error(e)
        .retryIf(predicate, attemptFixedRetryStrategy, { n, ex -> events << "retry$n" })
        .then { events << "then" }
    }, events.&add)

    then:
    events == ["retry1", "retry2", "retry3", e, "complete"]
  }

  def "do not retry failed promise if predicate does not match"() {
    when:
    def e = new NullPointerException("!")
    def i = new AtomicInteger()

    exec({
      Promise.sync { i.incrementAndGet() }
        .mapIf({ it < 3 }, { throw e })
        .retryIf(predicate, attemptFixedRetryStrategy, { n, e1 -> events << "retry$n" })
        .then(events.&add)
    }, events.&add)

    then:
    events == [e, "complete"]
  }

}
