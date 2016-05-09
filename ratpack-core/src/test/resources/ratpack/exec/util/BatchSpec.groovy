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
import ratpack.exec.ExecResult
import ratpack.exec.Promise
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class BatchSpec extends Specification {

  ExecHarness exec = ExecHarness.harness()
  def random = new Random()

  def "parallel yieldAll all success"() {
    given:
    List<Promise<String>> promises = (1..9).collect { i ->
      Blocking.get { sleep(random.nextInt(100)); "Promise $i" }
    }

    when:
    List<ExecResult<String>> result = exec.yieldSingle { e ->
      Batch.of(promises).parallel().yieldAll()
    }.valueOrThrow

    then:
    result.collect { it.valueOrThrow } == (1..9).collect { "Promise ${it}".toString() }
  }

  def "parallel yield all success"() {
    given:
    List<Promise<String>> promises = (1..9).collect { i ->
      Blocking.get { sleep(random.nextInt(100)); "Promise $i" }
    }

    when:
    List<String> result = exec.yieldSingle { e ->
      Batch.of(promises).parallel().yield()
    }.valueOrThrow

    then:
    result == (1..9).collect { "Promise ${it}".toString() }
  }

}
