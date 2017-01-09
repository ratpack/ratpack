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

import spock.util.concurrent.BlockingVariable

import java.time.Duration

class PromiseDeferSpec extends BaseExecutionSpec {

  def "can defer promise"() {
    when:
    def runner = new BlockingVariable<Runnable>()
    execHarness.controller.fork().onComplete { latch.countDown() }.start {
      Promise.async { f -> Thread.start { f.success("foo") } }.defer({ runner.set(it) }).then {
        events << it
      }
    }

    then:
    events == []

    when:
    runner.get().run()

    then:
    latch.await()
    events == ["foo"]
  }

  def "deferred promise can use promises even when promises are queued"() {
    when:
    def runner = new BlockingVariable<Runnable>(200)
    execHarness.controller.fork().onComplete { latch.countDown() }.start {
      Promise.value("foo").defer { runner.set(it) }.then {
        Promise.async { it.success("foo") }.then {
          events << "inner"
        }
      }
      Promise.value("outer").then { events << it }
    }

    then:
    events == []

    when:
    runner.get().run()

    then:
    latch.await()
    events == ["inner", "outer"]
  }

  def "can defer promise with duration"() {
    given:
    def delay = Duration.ofMillis(500)
    def totalDuration = Duration.ofMillis(0)

    when:
    exec {
      Promise.ofNull()
        .defer(delay)
        .time { totalDuration = it }
        .operation()
        .then()
    }

    then:
    totalDuration >= delay
  }

}
