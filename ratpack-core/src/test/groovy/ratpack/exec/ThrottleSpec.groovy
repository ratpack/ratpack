/*
 * Copyright 2014 the original author or authors.
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

import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

class ThrottleSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()

  PollingConditions polling = new PollingConditions(timeout: 5)

  def "can use throttle"() {
    def t = Throttle.ofSize(1)
    def v = execHarness.yield {
      execHarness.control.promise { it.success("foo") }.throttled(t)
    }

    expect:
    v.value == "foo"
  }

  def "can throttle operations"() {
    def q = new LinkedBlockingQueue<Fulfiller<Integer>>()

    def throttleSize = 5
    def jobs = 1000
    def t = Throttle.ofSize(throttleSize)
    def e = new ConcurrentLinkedQueue<Result<Integer>>()
    def latch = new CountDownLatch(jobs)

    when:
    jobs.times {
      execHarness.exec().onComplete { latch.countDown() }.start {
        def exec = it
        it.control.promise { q << it }.throttled(t).asResult {
          assert execHarness.control.execution.is(exec)
          e << it
        }
      }
    }

    then:
    polling.eventually {
      q.size() == t.size
      t.active == q.size()
      t.waiting == jobs - t.size
    }

    execHarness.exec().start { it.control.blocking { q.take().success(1) } then {} }

    polling.eventually {
      q.size() == t.size
      t.active == q.size()
      t.waiting == jobs - t.size - 1
    }

    execHarness.exec().start { e2 ->
      def n = jobs - 2 - throttleSize
      n.times {
        e2.control.blocking { q.take() } then {
          it.success(1)
        }
      }
    }

    polling.eventually {
      q.size() == t.size
      t.active == t.size
      t.waiting == 1
    }

    (throttleSize + 1).times {
      q.take().success(1)
    }

    polling.eventually {
      q.size() == 0
      t.active == 0
      t.waiting == 0
    }

  }

}
