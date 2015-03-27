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

import com.google.common.reflect.TypeToken
import org.springframework.context.support.StaticApplicationContext
import ratpack.func.Function
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
    !r.first(TypeToken.of(Object), Function.constant(null)).isPresent()
  }

  def "search with one item"() {
    given:
    TypeToken type = TypeToken.of(String)
    TypeToken other = TypeToken.of(Number)
    def value = "Something"

    beanFactory.registerSingleton("value", value)

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
    beanFactory.registerSingleton("a", a)
    beanFactory.registerSingleton("b", b)
    beanFactory.registerSingleton("c", c)
    beanFactory.registerSingleton("d", d)

    expect:
    r.first(string, Function.identity()).get() == a
    r.first(string, { s -> s.startsWith('B') ? s : null }).get() == b
    r.first(number, Function.identity()).get() == c
    r.first(number, { n -> n < 20 ? n : null }).get() == d
  }

  def "find first"() {
    given:
    def sameType = TypeToken.of(String)
    def differentType = TypeToken.of(Number)
    def value = "Something"
    beanFactory.registerSingleton("value", value)
    expect:
    r.first(sameType, Function.identity()).get() == value
    !r.first(sameType, Function.constant(null)).present
    !r.first(differentType, Function.identity()).present
    !r.first(differentType, Function.constant(null)).present
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
