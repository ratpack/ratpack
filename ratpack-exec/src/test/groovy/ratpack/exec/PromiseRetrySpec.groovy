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

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class PromiseRetrySpec extends BaseExecutionSpec {

  def "can retry failed promise and succeed"() {
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

  def "can retry failed promise and fail"() {
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

  def "can retry failed promise with fixed delay and succeed"() {
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

  def "can retry failed promise with fixed delay and fail"() {
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

  def "promise retry action is an execution segment"() {
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
}
