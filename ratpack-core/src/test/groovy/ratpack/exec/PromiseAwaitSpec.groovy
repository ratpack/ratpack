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

import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class PromiseAwaitSpec extends Specification {

  def execHarness = ExecHarness.harness()

  private <T> Promise<T> async(Result<T> t) {
    execHarness.promise { f ->
      Thread.start {
        f.accept(t)
      }
    }
  }

  def "can await promise"() {
    when:
    def r = execHarness.yield {
      it.blocking {
        async(Result.success(2)).await()
      }
    }

    then:
    r.valueOrThrow == 2
  }

  def "can await non async promise"() {
    when:
    def r = execHarness.yield {
      it.blocking {
        execHarness.promiseOf(2).await()
      }
    }

    then:
    r.valueOrThrow == 2
  }

  def "can await failed promise"() {
    when:
    def r = execHarness.yield {
      it.blocking {
        async(Result.error(new RuntimeException("!"))).await()
      }
    }.throwable

    then:
    r instanceof RuntimeException
    r.message == "!"
  }

  def "can nest awaits"() {
    when:
    def r = execHarness.yield { e ->
      execHarness.blocking {
        execHarness.blocking {
          async(Result.success(2)).map { it * 2 }.await()
        }.map {
          it * 2
        }.await()
      }
    }.valueOrThrow

    then:
    r == 8
  }

}
