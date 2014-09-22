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

import ratpack.registry.NotInRegistryException
import spock.lang.Specification

class SimpleMutableRegistrySpec extends Specification {

  def r = new SimpleMutableRegistry()

  def "empty mutable registry"() {
    expect:
    r.maybeGet(String) == null
    r.remove(String)

    when:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "add to registry"() {
    when:
    r.register("foo")

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
    r.register("foo")
    r.register("bar")

    then:
    r.get(String) == "foo"
    r.get(CharSequence) == "foo"
    r.getAll(String).toList() == ["foo", "bar"]
    r.getAll(CharSequence).toList() == ["foo", "bar"]

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
    r.registerLazy(Integer) { called = true; 2 }

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
