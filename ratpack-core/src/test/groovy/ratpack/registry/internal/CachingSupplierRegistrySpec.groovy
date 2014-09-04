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

import com.google.common.base.Function
import com.google.common.base.Predicates
import com.google.common.base.Supplier
import com.google.common.reflect.TypeToken
import ratpack.func.Action
import ratpack.registry.NotInRegistryException
import spock.lang.Specification

class CachingSupplierRegistrySpec extends Specification {
  def r = new CachingSupplierRegistryTestImpl()

  def "empty registry"() {
    expect:
    r.maybeGet(String) == null

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
    r.first(TypeToken.of(Object), Predicates.alwaysTrue()) == null
    r.first(TypeToken.of(Object), Predicates.alwaysFalse()) == null
    r.all(TypeToken.of(Object), Predicates.alwaysTrue()).toList() == []
    r.all(TypeToken.of(Object), Predicates.alwaysFalse()).toList() == []
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String)
    TypeToken other = TypeToken.of(Number)
    def value = "Something"

    r.register(value)

    expect:
    r.first(type, Predicates.alwaysTrue()) == value
    r.first(type, Predicates.alwaysFalse()) == null
    r.first(other, Predicates.alwaysTrue()) == null
    r.first(other, Predicates.alwaysFalse()) == null

    r.all(type, Predicates.alwaysTrue()) as List == [value]
    r.all(type, Predicates.alwaysFalse()) as List == []
    r.all(other, Predicates.alwaysTrue()) as List == []
    r.all(other, Predicates.alwaysFalse()) as List == []
  }

  def "search with multiple items"() {
    given:
    TypeToken string = TypeToken.of(String)
    TypeToken number = TypeToken.of(Number)
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    r.register(a)
    r.register(b)
    r.register(c)
    r.register(d)

    expect:
    r.first(string, Predicates.alwaysTrue()) == a
    r.first(string, { s -> s.startsWith('B') }) == b
    r.first(number, Predicates.alwaysTrue()) == c
    r.first(number, { n -> n < 20 }) == d

    r.all(string, Predicates.alwaysTrue()) as List == [a, b]
    r.all(string, { s -> s.startsWith('B') }) as List == [b]
    r.all(number, { n -> n < 50 })  as List == [c, d]
    r.all(number, Predicates.alwaysFalse()) as List == []
  }

  def "each with action"() {
    given:
    Action action = Mock()
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    r.register(value)

    when:
    r.each(sameType, Predicates.alwaysTrue(), action)

    then:
    1 * action.execute(value)

    when:
    r.each(sameType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)

    when:
    r.each(differentType, Predicates.alwaysTrue(), action)
    r.each(differentType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)
  }

  def "find first"() {
    given:
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    r.register(value)
    expect:
    r.first(sameType, Predicates.alwaysTrue()) == value
    r.first(sameType, Predicates.alwaysFalse()) == null
    r.first(differentType, Predicates.alwaysTrue()) == null
    r.first(differentType, Predicates.alwaysFalse()) == null
  }

  def "find all"() {
    given:
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    r.register(value)
    expect:
    r.all(sameType, Predicates.alwaysTrue()).toList() == [value]
    r.all(sameType, Predicates.alwaysFalse()).toList() == []
    r.all(differentType, Predicates.alwaysTrue()).toList() == []
    r.all(differentType, Predicates.alwaysFalse()).toList() == []
  }

  def "lookups are cached when all or first method is used and it's possible to invalidate the cache"() {
    given:
    TypeToken string = TypeToken.of(String)
    def a = "A"
    def b = "B"
    def c = "C"
    def d = "D"
    Function<TypeToken<?>, List<? extends Supplier<?>>> supplierFunc = Mock()
    def registry = new CachingSupplierRegistry(supplierFunc)
    when:
    def result = registry.all(string, Predicates.alwaysTrue()) as List
    def result2 = registry.all(string, Predicates.alwaysTrue()) as List
    def sresult = registry.first(string, Predicates.alwaysTrue())
    def sresult2 = registry.first(string, Predicates.alwaysTrue())
    then:
    1 * supplierFunc.apply(string) >> { TypeToken<?> input ->
      [{-> a} as Supplier, {-> b} as Supplier]
    }
    0 * supplierFunc._
    result == [a, b]
    result2 == [a, b]
    sresult == a
    sresult2 == a

    when: "cache is invalidated"
    registry.invalidateAll()
    def sresult3 = registry.first(string, Predicates.alwaysTrue())
    def result3 = registry.all(string, Predicates.alwaysTrue()) as List

    then: "lookup is made again"
    1 * supplierFunc.apply(string) >> { TypeToken<?> input ->
      [{-> c} as Supplier, {-> d} as Supplier]
    }
    0 * supplierFunc._
    sresult3 == c
    result3 == [c, d]
  }

  def "lookups are cached when getAll or get method is used and it's possible to invalidate the cache"() {
    given:
    TypeToken string = TypeToken.of(String)
    def a = "A"
    def b = "B"
    def c = "C"
    def d = "D"
    Function<TypeToken<?>, List<? extends Supplier<?>>> supplierFunc = Mock()
    def registry = new CachingSupplierRegistry(supplierFunc)
    when:
    def result = registry.getAll(string) as List
    def result2 = registry.getAll(string) as List
    def sresult = registry.get(string)
    def sresult2 = registry.get(string)

    then:
    1 * supplierFunc.apply(string) >> { TypeToken<?> input ->
      [{-> a} as Supplier, {-> b} as Supplier]
    }
    0 * supplierFunc._
    result == [a, b]
    result2 == [a, b]
    sresult == a
    sresult2 == a

    when: "cache is invalidated"
    registry.invalidateAll()
    def sresult3 = registry.get(string)
    def result3 = registry.getAll(string) as List

    then: "lookup is made again"
    1 * supplierFunc.apply(string) >> { TypeToken<?> input ->
      [{-> c} as Supplier, {-> d} as Supplier]
    }
    0 * supplierFunc._
    sresult3 == c
    result3 == [c, d]
  }
}
