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

package ratpack.guice

import com.google.common.base.Predicates
import com.google.common.reflect.TypeToken
import com.google.inject.Binder
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Provider
import ratpack.groovy.internal.ClosureUtil
import ratpack.registry.Registry
import spock.lang.Specification

class GuiceRegistrySpec extends Specification {

  Injector injector = Mock(Injector)
  @Delegate
  Registry registry = Guice.registry(injector)

  // Used to back the impl of injector modk methods
  Injector realInjector

  void realInjector(@DelegatesTo(Binder) Closure<?> closure) {
    realInjector = com.google.inject.Guice.createInjector(new Module() {
      @Override
      void configure(Binder binder) {
        ClosureUtil.configureDelegateFirst(binder, closure)
      }
    })
  }

  def "lookups are cached"() {
    given:
    realInjector {
      bind(String).toInstance("foo")
    }

    when:
    registry.get(String) == "foo"
    registry.get(String) == "foo"

    then:
    1 * injector.getAllBindings() >> realInjector.getAllBindings()
  }

  def "cached providers can be dynamic"() {
    given:
    def i = 0
    realInjector {
      bind(String).toProvider(new Provider<String>() {
        @Override
        String get() {
          "foo${i++}"
        }
      })
    }

    when:
    registry.get(String) == "foo0"
    registry.get(String) == "foo1"

    then:
    1 * injector.getAllBindings() >> realInjector.getAllBindings()
  }

  def "get all returns all that are assignment compatible"() {
    given:
    realInjector {
      bind(String).toInstance("string")
      bind(GString).toInstance("${'gstring'}")
      bind(CharSequence).toInstance("charsequence")
    }

    when:
    registry.getAll(CharSequence) == ["string", "gstring", "charsequence"]
    registry.getAll(CharSequence) == ["string", "gstring", "charsequence"]

    then:
    1 * injector.getAllBindings() >> realInjector.getAllBindings()
  }

  def "search with multiple items"() {
    given:
    TypeToken charseq = TypeToken.of(CharSequence)
    TypeToken number = TypeToken.of(Number)
    def a = "A"
    def b = "B"
    def c = 42
    def d = 16
    realInjector {
      bind(String).toInstance(a)
      bind(CharSequence).toInstance(b)
      bind(Number).toInstance(c)
      bind(Integer).toInstance(d)
    }
    injector.getAllBindings() >> realInjector.getAllBindings()

    expect:
    registry.first(charseq, Predicates.alwaysTrue()).get() == a
    registry.first(charseq, { CharSequence s -> s.startsWith('B') }).get() == b
    registry.first(number, Predicates.alwaysTrue()).get() == c
    registry.first(number, { Number n -> n < 20 }).get() == d

    registry.all(charseq, Predicates.alwaysTrue()) as List == [a, b]
    registry.all(charseq, { s -> s.startsWith('B') }) as List == [b]
    registry.all(number, { n -> n < 50 }) as List == [c, d]
    registry.all(number, Predicates.alwaysFalse()) as List == []
  }

  def "equals and hashCode should be implemented"() {
    given:
    def otherRegistry = ratpack.guice.Guice.registry(injector)
    expect:
    otherRegistry.equals(registry)
    registry.equals(otherRegistry)
    !registry.equals(null)
    !registry.equals(new Object())
    registry.equals(registry)
    otherRegistry.hashCode() == registry.hashCode()
  }
}
