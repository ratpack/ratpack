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

class ExecErrorPropagationSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()

  def "failure in nested segment propagates through outer layer"() {
    when:
    harness.yield {
      Promise.value(1)
        .next { Promise.error(new IllegalArgumentException()).then {} }
        .mapError { throw new IllegalStateException() }
    }.valueOrThrow

    then:
    thrown IllegalStateException
  }

  def "failure in nested segment continuation propagates through outer layer"() {
    when:
    harness.yield {
      Promise.value(1)
        .next { Promise.value(1).then { throw new IllegalArgumentException() } }
        .mapError { throw new IllegalStateException() }
    }.valueOrThrow

    then:
    thrown IllegalStateException
  }

  def "failure propagates up several layers"() {
    when:
    harness.yield {
      Promise.value(1)
        .next {
        Promise.value(1)
          .next { Promise.value(1).then { throw new IllegalArgumentException() } }
          .then()
      }
      .mapError { throw new IllegalStateException() }
    }.valueOrThrow

    then:
    thrown IllegalStateException
  }

  def "failure can be wrapped and propagated"() {
    when:
    harness.yield {
      Promise.value(1)
        .next {
        Promise.value(1)
          .next { Promise.value(1).then { throw new IllegalArgumentException() } }
          .mapError { throw new UnsupportedOperationException() }
          .then()
      }
      .mapError { throw new IllegalStateException(it) }
    }.valueOrThrow

    then:
    def e = thrown IllegalStateException
    e.cause instanceof UnsupportedOperationException
  }

  def "propagation can be halted"() {
    expect:
    harness.yield {
      Promise.value(1)
        .flatMap {
        Promise.value(1)
          .next { Promise.value(1).then { throw new IllegalArgumentException() } }
          .mapError { 2 }
      }
      .mapError { throw new IllegalStateException(it) }
    }.valueOrThrow == 2
  }

  def "error is propagated when converting to operation"() {
    expect:
    harness.yield {
      Promise.error(new RuntimeException("!"))
        .operation()
        .onError { throw new RuntimeException("!!") }
        .promise()
    }.throwable.message == "!!"
  }

}
