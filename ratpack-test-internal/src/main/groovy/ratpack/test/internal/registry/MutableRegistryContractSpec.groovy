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

package ratpack.test.internal.registry

import ratpack.func.Action
import ratpack.registry.MutableRegistry
import ratpack.registry.NotInRegistryException
import ratpack.registry.RegistrySpec


abstract class MutableRegistryContractSpec extends RegistryContractSpec {

  @Override
  abstract MutableRegistry build(Action<? super RegistrySpec> spec)

  def "empty mutable registry"() {
    given:
    def r = build { }

    expect:
    !r.maybeGet(String).isPresent()
    r.remove(String)

    when:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "add to registry"() {
    given:
    def r = build { }

    when:
    r.add("foo")

    then:
    r.get(String) == "foo"
    r.get(CharSequence) == "foo"
    r.getAll(String).toList() == ["foo"]
    r.getAll(CharSequence).toList() == ["foo"]

    when:
    r.remove(String)
    r.getAll(String) == []
    r.getAll(CharSequence) == []

    and:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "ordering"() {
    given:
    def r = build { }

    when:
    r.add("foo")
    r.add("bar")

    then:
    r.get(String) == "bar"
    r.get(CharSequence) == "bar"
    r.getAll(String).toList() == ["bar", "foo"]
    r.getAll(CharSequence).toList() == ["bar", "foo"]

    when:
    r.remove(String)
    r.getAll(String) == []
    r.getAll(CharSequence) == []

    and:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "laziness"() {
    given:
    def r = build { }
    def called = false
    r.addLazy(Integer) { called = true; 2 }

    when:
    r.maybeGet(String) == null

    then:
    !called

    when:
    r.get(Number) == 2

    then:
    called
  }

  def "locking"() {
    given:
    def r = build { }
    r.add("foo")
    r.add("bar")

    when:
    def r2 = r.asImmutable()

    then:
    r2.get(String) == "bar"
    r2.get(CharSequence) == "bar"
    r2.getAll(String).toList() == ["bar", "foo"]
    r2.getAll(CharSequence).toList() == ["bar", "foo"]
  }
}
