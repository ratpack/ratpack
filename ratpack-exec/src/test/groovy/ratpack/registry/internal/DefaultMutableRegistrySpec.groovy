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

package ratpack.registry.internal

import ratpack.func.Action
import ratpack.registry.NotInRegistryException
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.test.internal.registry.RegistryContractSpec

class DefaultMutableRegistrySpec extends RegistryContractSpec {

  def r = Registry.mutable()

  @Override
  Registry build(Action<? super RegistrySpec> spec) {
    def r = new DefaultMutableRegistry()
    spec.execute(r)
    r
  }

  def "empty mutable registry"() {
    expect:
    !r.maybeGet(String).isPresent()
    r.remove(String)

    when:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "add to registry"() {
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
}
