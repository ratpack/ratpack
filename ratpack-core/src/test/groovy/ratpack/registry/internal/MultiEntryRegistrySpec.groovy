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

import com.google.common.base.Predicates
import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import ratpack.func.Action
import spock.lang.Specification

import javax.sql.rowset.Predicate

class MultiEntryRegistrySpec extends Specification {
  def "search empty registry"() {
    given:
    def r = new MultiEntryRegistry(ImmutableList.of())
    Action action = Mock() {
      0 * execute(_)
    }

    expect:
    r.first(TypeToken.of(Object.class), Predicates.alwaysTrue()) == null
    r.first(TypeToken.of(Object.class), Predicates.alwaysFalse()) == null
    r.all(TypeToken.of(Object.class), Predicates.alwaysTrue()) == []
    r.all(TypeToken.of(Object.class), Predicates.alwaysFalse()) == []

    r.first(TypeToken.of(Object.class), Predicates.alwaysTrue(), action) == false
    r.first(TypeToken.of(Object.class), Predicates.alwaysFalse(), action) == false
    r.each(TypeToken.of(Object.class), Predicates.alwaysTrue(), action) == false
    r.each(TypeToken.of(Object.class), Predicates.alwaysFalse(), action) == false
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String.class)
    TypeToken other = TypeToken.of(Number.class)
    def value = "Something"

    def r = new MultiEntryRegistry(ImmutableList.of(new DefaultRegistryEntry(type, value)))

    Action action = Mock() {
      2 * execute(value)
    }

    expect:
    r.first(type, Predicates.alwaysTrue()) == value
    r.first(type, Predicates.alwaysFalse()) == null
    r.first(other, Predicates.alwaysTrue()) == null
    r.first(other, Predicates.alwaysFalse()) == null

    r.all(type, Predicates.alwaysTrue()) == [value]
    r.all(type, Predicates.alwaysFalse()) == []
    r.all(other, Predicates.alwaysTrue()) == []
    r.all(other, Predicates.alwaysFalse()) == []

    r.first(type, Predicates.alwaysTrue(), action) == true
    r.first(type, Predicates.alwaysFalse(), action) == false
    r.first(other, Predicates.alwaysTrue(), action) == false
    r.first(other, Predicates.alwaysFalse(), action) == false

    r.each(type, Predicates.alwaysTrue(), action) == true
    r.each(type, Predicates.alwaysFalse(), action) == false
    r.each(other, Predicates.alwaysTrue(), action) == false
    r.each(other, Predicates.alwaysFalse(), action) == false
  }

  def "search with multiple items"() {
    given:
    TypeToken string = TypeToken.of(String.class)
    TypeToken number = TypeToken.of(Number.class)
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    def r = new MultiEntryRegistry(ImmutableList.of(new DefaultRegistryEntry(string, a), new DefaultRegistryEntry(string, b),
      new DefaultRegistryEntry(number, c), new DefaultRegistryEntry(number, d)))

    Action action = Mock() {
      2 * execute(a)
      1 * execute(b)
      1 * execute(c)
      1 * execute(d)
    }

    expect:
    r.first(string, Predicates.alwaysTrue()) == a
    r.first(string, {s -> s.startsWith('B')}) == b
    r.first(number, Predicates.alwaysTrue()) == c
    r.first(number, {n -> n < 20}) == d

    r.all(string, Predicates.alwaysTrue()) == [a, b]
    r.all(string, {s -> s.startsWith('B')}) == [b]
    r.all(number, {n -> n < 50}) == [c, d]
    r.all(number, Predicates.alwaysFalse()) == []

    r.first(string, Predicates.alwaysTrue(), action) == true
    r.first(number, {n -> n > 20}, action) == true
    r.first(string, Predicates.alwaysFalse(), action) == false

    r.each(string, Predicates.alwaysTrue(), action) == true
    r.each(number, {n -> n < 30}, action) == true
    r.each(string, Predicates.alwaysFalse(), action) == false
  }
}
