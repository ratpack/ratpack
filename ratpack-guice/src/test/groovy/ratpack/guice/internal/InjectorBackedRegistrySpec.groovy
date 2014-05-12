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

package ratpack.guice.internal

import com.google.inject.Binder
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import ratpack.groovy.internal.ClosureUtil
import ratpack.registry.Registry
import spock.lang.Specification

class InjectorBackedRegistrySpec extends Specification {

  Injector injector = Mock(Injector)
  @Delegate
  Registry registry = new InjectorBackedRegistry(injector)

  // Used to back the impl of injector modk methods
  Injector realInjector

  void realInjector(@DelegatesTo(Binder) Closure<?> closure) {
    realInjector = Guice.createInjector(new Module() {
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
      bind(String).toProvider {
        "foo${i++}"
      }
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

}
