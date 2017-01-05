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

import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Duration

class PromiseTimingSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()

  def "can time success promise"() {
    expect:
    Duration time
    execHarness.yield {
      Promise.sync { sleep(500); 1 }.next { sleep(500) }.time { time = it }
    }
    time.seconds >= 1
  }

  def "can time failed promise"() {
    expect:
    Duration time
    execHarness.yield {
      Promise.sync { sleep(500); throw new NullPointerException() }.next { sleep(500) }.time { time = it }
    }
    time.toMillis() >= 500
  }

  def "can time complete promise"() {
    expect:
    Duration time
    execHarness.yield {
      Execution.sleep(Duration.ofMillis(500)).next(Execution.sleep(Duration.ofMillis(500))).promise().time { time = it }
    }

    time.toMillis() >= 500
  }

  def "time includes yield time"() {
    expect:
    Duration time
    execHarness.yield {
      Promise.sync { sleep(500); 1 }.next { sleep(500) }.onYield { sleep(500) }.time { time = it }
    }
    time.toMillis() >= 1500
  }

}
