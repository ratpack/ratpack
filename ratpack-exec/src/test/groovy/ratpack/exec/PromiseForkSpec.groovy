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

import ratpack.func.Pair
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CyclicBarrier

class PromiseForkSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()

  def "can fork successful"() {
    when:
    def b = new CyclicBarrier(2)
    def r = harness.yield {
      Blocking.get {
        b.await()
        1
      }.right(Blocking.get {
        b.await()
        2
      }.fork())
    }.valueOrThrow

    then:
    r == Pair.of(1, 2)
  }

  def "can fork failed"() {
    when:
    def b = new CyclicBarrier(2)
    harness.yield {
      Blocking.get {
        b.await()
        1
      }.right(Blocking.get {
        b.await()
        throw new IllegalStateException("!")
      }.fork())
    }.valueOrThrow

    then:
    def e = thrown IllegalStateException
    e.message == "!"
  }

  def "can configure forked exec"() {
    when:
    def b = new CyclicBarrier(2)
    def r = harness.yield {
      Blocking.get {
        b.await()
        1
      }.right(Blocking.get {
        b.await()
        Execution.current().get(Integer)
      }.fork {
        it.register {
          it.add(Integer, 2)
        }
      })
    }.valueOrThrow

    then:
    r == Pair.of(1, 2)
  }

}
