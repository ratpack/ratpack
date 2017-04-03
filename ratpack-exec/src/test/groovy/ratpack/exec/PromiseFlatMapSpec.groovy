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

import spock.lang.Unroll

class PromiseFlatMapSpec extends BaseExecutionSpec {

  def "can flat map promise"() {
    when:
    exec({ e ->
      Blocking.get { "foo" }
        .flatMap { s -> Blocking.get { s + "-bar" } }
        .map { it.toUpperCase() }
        .then { events << it }
    })

    then:
    events == ["FOO-BAR", "complete"]
  }

  @Unroll
  def "can flatMapIf promise when the predicate is #predicate"() {
    when:
    exec {
      Blocking.get { originalValue }
        .flatMapIf( { s -> s == "foo" }, { s -> Blocking.get { s + "-true" } } )
        .then { events << it }
    }

    then:
    events == [mappedValue, "complete"]

    where:
    originalValue | mappedValue | predicate
    "foo"         | "foo-true"  | true
    "bar"         | "bar" | false
  }

  @Unroll
  def "can flatMapIfOrElse promise when the predicate is #predicate"() {
    when:
    exec {
      Blocking.get { originalValue }
        .flatMapIf( { s -> s == "foo" }, { s -> Blocking.get { s + "-true" } } , { s -> Blocking.get { s + "-false" } } )
        .then { events << it }
    }

    then:
    events == [mappedValue, "complete"]

    where:
    originalValue | mappedValue | predicate
    "foo"         | "foo-true"  | true
    "bar"         | "bar-false" | false
  }

  def "can use other promise with flatMap"() {
    when:
    exec { e ->
      Blocking.get {
        "foo"
      } flatMap {
        Promise.async { f -> Thread.start { f.success("foo") } }
      } then {
        events << it
      }
    }

    then:
    events == ["foo", "complete"]
  }

  def "errors are propagated down flatmap chain"() {
    given:
    def ex = new RuntimeException("!")

    when:
    exec { e ->
      Promise.async { it.error(ex) }
        .map {}
        .flatMap { Blocking.get { "foo" } }
        .onError { events << it }
        .then { throw new IllegalStateException("cant get here") }
    }

    then:
    events == [ex, "complete"]
  }

}
