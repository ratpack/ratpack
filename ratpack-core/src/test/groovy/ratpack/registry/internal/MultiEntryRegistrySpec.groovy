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
import spock.lang.Specification

class MultiEntryRegistrySpec extends Specification {
  def "search empty registry"() {
    given:
    def r = new MultiEntryRegistry(ImmutableList.of())

    expect:
    !r.first(TypeToken.of(Object), Predicates.alwaysTrue()).present
    !r.first(TypeToken.of(Object), Predicates.alwaysFalse()).present
    r.all(TypeToken.of(Object), Predicates.alwaysTrue()) == []
    r.all(TypeToken.of(Object), Predicates.alwaysFalse()) == []
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String)
    TypeToken other = TypeToken.of(Number)
    def value = "Something"

    def r = new MultiEntryRegistry(ImmutableList.of(new DefaultRegistryEntry(type, value)))

    expect:
    r.first(type, Predicates.alwaysTrue()).get() == value
    !r.first(type, Predicates.alwaysFalse()).present
    !r.first(other, Predicates.alwaysTrue()).present
    !r.first(other, Predicates.alwaysFalse()).present

    r.all(type, Predicates.alwaysTrue()) == [value]
    r.all(type, Predicates.alwaysFalse()) == []
    r.all(other, Predicates.alwaysTrue()) == []
    r.all(other, Predicates.alwaysFalse()) == []
  }

  def "search with multiple items"() {
    given:
    TypeToken string = TypeToken.of(String)
    TypeToken number = TypeToken.of(Number)
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    def r = new MultiEntryRegistry(ImmutableList.of(new DefaultRegistryEntry(string, a), new DefaultRegistryEntry(string, b),
      new DefaultRegistryEntry(number, c), new DefaultRegistryEntry(number, d)))

    expect:
    r.first(string, Predicates.alwaysTrue()).get() == a
    r.first(string, { s -> s.startsWith('B') }).get() == b
    r.first(number, Predicates.alwaysTrue()).get() == c
    r.first(number, { n -> n < 20 }).get() == d

    r.all(string, Predicates.alwaysTrue()) == [a, b]
    r.all(string, { s -> s.startsWith('B') }) == [b]
    r.all(number, { n -> n < 50 }) == [c, d]
    r.all(number, Predicates.alwaysFalse()) == []
  }
}
