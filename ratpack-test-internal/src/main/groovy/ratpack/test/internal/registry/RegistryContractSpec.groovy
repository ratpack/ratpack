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

package ratpack.test.internal.registry

import ratpack.func.Action
import ratpack.func.Function
import ratpack.registry.NotInRegistryException
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import spock.lang.Specification

abstract class RegistryContractSpec extends Specification {

  abstract Registry build(Action<? super RegistrySpec> spec)

  def "empty registry"() {
    expect:
    !build {}.maybeGet(String).present
    when:
    !build {}.get(String)

    then:
    thrown NotInRegistryException
  }

  def "get and getAll should support implemented interfaces besides actual class"() {
    when:
    def r = build { it.add("foo") }

    then:
    r.get(String) == "foo"
    r.get(CharSequence) == "foo"
    r.getAll(String).toList() == ["foo"]
    r.getAll(CharSequence).toList() == ["foo"]
  }

  def "search for one item"() {
    given:
    def r = build {
      it.add("foo")
    }

    expect:
    r.first(String, Function.identity()).get() == "foo"
    !r.first(String, Function.constant(null)).present
    !r.first(Integer, Function.identity()).present
    !r.first(Integer, Function.constant(null)).present
  }

  def "search with multiple items"() {
    given:
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    def r = build {
      it
        .add(a)
        .add(b)
        .add(c)
        .add(d)
    }

    expect:
    r.first(String, Function.identity()).get() == b
    r.first(String, { s -> s.startsWith('A') ? s : null }).get() == a
    r.first(Number, Function.identity()).get() == d
    r.first(Number, { n -> n > 20 ? n : null }).get() == c
  }

  def "find first"() {
    given:
    def r = build { it.add "foo" }

    expect:
    r.first(String, Function.identity()).get() == "foo"
    !r.first(String, Function.constant(null)).present
    !r.first(Number, Function.identity()).present
    !r.first(Number, Function.constant(null)).present
  }

  def "get all returns all that are assignment compatible"() {
    when:
    def registry = build {
      it
        .add(String, "string")
        .add(GString, "${'gstring'}")
        .add(CharSequence, "charsequence")
    }

    then:
    registry.getAll(CharSequence)*.toString() == ["charsequence", "gstring", "string"]
  }
}
