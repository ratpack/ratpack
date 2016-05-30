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

import com.google.inject.Binder
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Provider
import com.google.inject.Provides
import com.google.inject.multibindings.OptionalBinder
import ratpack.api.Nullable
import ratpack.func.Action
import ratpack.groovy.internal.ClosureUtil
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.server.ServerConfig
import ratpack.test.internal.registry.RegistryContractSpec

class GuiceRegistrySpec extends RegistryContractSpec {

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

  @Override
  Registry build(Action<? super RegistrySpec> spec) {
    Guice.registry(
      Guice.buildInjector(Registry.single(ServerConfig.embedded().build()), spec) { Module it ->
        com.google.inject.Guice.createInjector(it)
      }
    )
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
    1 * injector.getBindings() >> realInjector.getBindings()
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
    1 * injector.getBindings() >> realInjector.getBindings()
  }

  def "equals and hashCode should be implemented"() {
    given:
    def otherRegistry = Guice.registry(injector)
    expect:
    otherRegistry.equals(registry)
    registry.equals(otherRegistry)
    !registry.equals(null)
    !registry.equals(new Object())
    registry.equals(registry)
    otherRegistry.hashCode() == registry.hashCode()
  }

  def "can use providers that return null"() {
    given:
    Registry reg = Guice.registry(com.google.inject.Guice.createInjector(new Module() {
      @Override
      void configure(Binder binder) { }

      @Provides
      String stringProvider() {
        return null
      }
    }))

    expect:
    !reg.maybeGet(String).isPresent()
  }

  def "can use OptionalBinder"() {
    given:
    Registry reg = Guice.registry(com.google.inject.Guice.createInjector(new Module() {
      @Override
      void configure(Binder binder) {
        OptionalBinder.newOptionalBinder(binder, String)
      }

      @Provides
      String stringProvider() {
        return null
      }
    }))

    expect:
    !reg.maybeGet(String).isPresent()
  }

  def "can use providers that return Optional"() {
    given:
    Registry reg = Guice.registry(com.google.inject.Guice.createInjector(new Module() {
      @Override
      void configure(Binder binder) { }

      @Provides
      Optional<String> stringProvider() {
        return Optional.empty()
      }
    }))

    expect:
    !reg.maybeGet(String).isPresent()
  }

  def "can inject null using ratpack's Nullable annotation"() {
    given:
    Registry reg = Guice.registry(com.google.inject.Guice.createInjector(new Module() {
      @Override
      void configure(Binder binder) {
        OptionalBinder.newOptionalBinder(binder, Integer)
      }

      @Provides
      Integer integerProvider() {
        return null
      }

      @Provides
      String stringProvider(@Nullable Integer i) {
        i == null ? "null" : i
      }
    }))

    expect:
    reg.get(String) == "null"
  }

}
