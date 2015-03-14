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

import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import ratpack.func.Function
import spock.lang.Specification

class MultiEntryRegistrySpec extends Specification {
  def "search empty registry"() {
    given:
    def r = new MultiEntryRegistry(ImmutableList.of())

    expect:
    !r.first(TypeToken.of(Object), Function.identity()).present
    !r.first(TypeToken.of(Object), Function.constant(null)).present
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String)
    TypeToken other = TypeToken.of(Number)
    def value = "Something"

    def r = new MultiEntryRegistry(ImmutableList.of(new DefaultRegistryEntry(type, value)))

    expect:
    r.first(type, Function.identity()).get() == value
    !r.first(type, Function.constant(null)).present
    !r.first(other, Function.identity()).present
    !r.first(other, Function.constant(null)).present
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
    r.first(string, Function.identity()).get() == a
    r.first(string, { s -> s.startsWith('B') ? s : null }).get() == b
    r.first(number, Function.identity()).get() == c
    r.first(number, { n -> n < 20 ? n : null }).get() == d
  }
}
