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

import com.google.common.base.Supplier
import com.google.common.reflect.TypeToken
import ratpack.func.Function
import ratpack.registry.NotInRegistryException
import ratpack.registry.RegistryBacking
import ratpack.util.Types
import spock.lang.Specification

class CachingBackedRegistrySpec extends Specification {
  def r = new CachingBackedRegistryTestImpl()

  def "empty registry"() {
    expect:
    !r.maybeGet(String).present

    when:
    r.get(String)

    then:
    thrown NotInRegistryException
  }

  def "get and getAll should support implemented interfaces besides actual class"() {
    when:
    r.register("foo")

    then:
    r.get(String) == "foo"
    r.get(CharSequence) == "foo"
    r.getAll(String).toList() == ["foo"]
    r.getAll(CharSequence).toList() == ["foo"]
  }

  def "search empty registry"() {
    expect:
    !r.first(Types.token(Object), Function.identity()).present
    !r.first(Types.token(Object), Function.constant(null)).present
  }

  def "search with one item"() {
    given:
    TypeToken type = Types.token(String)
    TypeToken other = Types.token(Number)
    def value = "Something"

    r.register(value)

    expect:
    r.first(type, Function.identity()).get() == value
    !r.first(type, Function.constant(null)).present
    !r.first(other, Function.identity()).present
    !r.first(other, Function.constant(null)).present
  }

  def "search with multiple items"() {
    given:
    TypeToken string = Types.token(String)
    TypeToken number = Types.token(Number)
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    r.register(a)
    r.register(b)
    r.register(c)
    r.register(d)

    expect:
    r.first(string, Function.identity()).get() == a
    r.first(string, { s -> s.startsWith('B') ? s : null }).get() == b
    r.first(number, Function.identity()).get() == c
    r.first(number, { n -> n < 20 ? n : null }).get() == d
  }

  def "find first"() {
    given:
    def sameType = Types.token(String)
    def differentType = Types.token(Number)
    def value = "Something"
    r.register(value)
    expect:
    r.first(sameType, Function.identity()).get() == value
    !r.first(sameType, Function.constant(null)).present
    !r.first(differentType, Function.identity()).present
    !r.first(differentType, Function.constant(null)).present
  }


  def "lookups are cached when all or first method is used"() {
    given:
    TypeToken string = Types.token(String)
    def a = "A"
    def b = "B"
    RegistryBacking supplierFunc = Mock()
    def registry = new CachingBackedRegistry(supplierFunc)
    when:
    def sresult = registry.first(string, Function.identity())
    def sresult2 = registry.first(string, Function.identity())
    then:
    1 * supplierFunc.provide(string) >> { TypeToken<?> input ->
      [{ -> a } as Supplier, { -> b } as Supplier]
    }
    0 * supplierFunc._
    sresult.get() == a
    sresult2.get() == a
  }

  def "lookups are cached when getAll or get method is used"() {
    given:
    TypeToken string = Types.token(String)
    def a = "A"
    def b = "B"
    RegistryBacking supplierFunc = Mock()
    def registry = new CachingBackedRegistry(supplierFunc)
    when:
    def result = registry.getAll(string) as List
    def result2 = registry.getAll(string) as List
    def sresult = registry.get(string)
    def sresult2 = registry.get(string)

    then:
    1 * supplierFunc.provide(string) >> { TypeToken<?> input ->
      [{ -> a } as Supplier, { -> b } as Supplier]
    }
    0 * supplierFunc._
    result == [a, b]
    result2 == [a, b]
    sresult == a
    sresult2 == a
  }
}
