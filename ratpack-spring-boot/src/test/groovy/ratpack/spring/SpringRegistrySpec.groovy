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

package ratpack.spring

import com.google.common.base.Predicates
import com.google.common.reflect.TypeToken
import org.springframework.context.support.StaticApplicationContext
import ratpack.func.Action
import ratpack.registry.NotInRegistryException
import spock.lang.Specification

class SpringRegistrySpec extends Specification {
  def appContext = new StaticApplicationContext()
  def r = Spring.spring(appContext)
  def beanFactory = appContext.getBeanFactory()

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
    beanFactory.registerSingleton("foo", "foo")

    then:
    r.get(String) == "foo"
    r.get(CharSequence) == "foo"
    r.getAll(String).toList() == ["foo"]
    r.getAll(CharSequence).toList() == ["foo"]
  }

  def "search empty registry with always false predicate"() {
    expect:
    !r.first(TypeToken.of(Object), Predicates.alwaysFalse()).isPresent()
    r.all(TypeToken.of(Object), Predicates.alwaysFalse()) as List == []
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String)
    TypeToken other = TypeToken.of(Number)
    def value = "Something"

    beanFactory.registerSingleton("value", value)

    expect:
    r.first(type, Predicates.alwaysTrue()).get() == value
    !r.first(type, Predicates.alwaysFalse()).present
    !r.first(other, Predicates.alwaysTrue()).present
    !r.first(other, Predicates.alwaysFalse()).present

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
    beanFactory.registerSingleton("a", a)
    beanFactory.registerSingleton("b", b)
    beanFactory.registerSingleton("c", c)
    beanFactory.registerSingleton("d", d)

    expect:
    r.first(string, Predicates.alwaysTrue()).get() == a
    r.first(string, { s -> s.startsWith('B') }).get() == b
    r.first(number, Predicates.alwaysTrue()).get() == c
    r.first(number, { n -> n < 20 }).get() == d

    r.all(string, Predicates.alwaysTrue()) as List == [a, b]
    r.all(string, { s -> s.startsWith('B') }) as List == [b]
    r.all(number, { n -> n < 50 }) as List == [c, d]
    r.all(number, Predicates.alwaysFalse()) as List == []
  }

  def "each with action"() {
    given:
    Action action = Mock()
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    beanFactory.registerSingleton("value", value)

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
    beanFactory.registerSingleton("value", value)
    expect:
    r.first(sameType, Predicates.alwaysTrue()).get() == value
    !r.first(sameType, Predicates.alwaysFalse()).present
    !r.first(differentType, Predicates.alwaysTrue()).present
    !r.first(differentType, Predicates.alwaysFalse()).present
  }

  def "find all"() {
    given:
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    beanFactory.registerSingleton("value", value)
    expect:
    r.all(sameType, Predicates.alwaysTrue()).toList() == [value]
    r.all(sameType, Predicates.alwaysFalse()).toList() == []
    r.all(differentType, Predicates.alwaysTrue()).toList() == []
    r.all(differentType, Predicates.alwaysFalse()).toList() == []
  }

  def "equals and hashCode should be implemented"() {
    given:
    def otherRegistry = Spring.spring(appContext)
    expect:
    otherRegistry.equals(r)
    r.equals(otherRegistry)
    r.equals(null) == false
    r.equals(new Object()) == false
    r.equals(r)
    otherRegistry.hashCode() == r.hashCode()
  }
}