/*
 * Copyright 2013 the original author or authors.
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

import com.google.inject.AbstractModule
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Provider

class BindingsSpecSpec extends RatpackGroovyDslSpec {

  static interface Type1 {}

  static class Type1Impl1 implements Type1 {}

  static class Type1Impl2 implements Type1 {}

  static interface Type2 {}

  static class Type2Impl1 implements Type2 {}

  static class Type2Impl2 implements Type2 {}

  static class Type2Provider implements Provider<Type2> {
    Type2 get() {
      new Type2Impl2()
    }
  }

  static class SomeType {}

  def "can bind via module registry"() {
    when:
    bindings {
      // direct bindings always override module bindings
      bind SomeType
      bind Type1, Type1Impl2
      providerType Type2, Type2Provider

      // regardless of module registration order
      module new AbstractModule() {
        protected void configure() {
          bind(Type1).to(Type1Impl1)
          bind(Type2).to(Type2Impl1)
        }
      }

    }
    handlers {
      get("classDirect") { SomeType someType ->
        response.send someType.class.name
      }
      get("publicType") { Type1 someType ->
        response.send someType.class.name
      }
      get("provider") { Type2 someType ->
        response.send someType.class.name
      }
    }

    then:
    getText("classDirect") == SomeType.name
    getText("publicType") == Type1Impl2.name
    getText("provider") == Type2Impl2.name
  }


}
