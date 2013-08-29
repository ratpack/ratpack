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

package org.ratpackframework.guice

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Module
import org.ratpackframework.test.internal.RatpackGroovyDslSpec
import org.ratpackframework.util.Transformer

import static com.google.inject.Guice.createInjector

class GuiceBootstrappingSpec extends RatpackGroovyDslSpec {

  static class ServiceOne {
    String name
  }

  static class ServiceTwo {
    String name
  }

  @Override
  Transformer<Module, Injector> createInjectorFactory() {
    Injector parentInjector = createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ServiceOne).toInstance(new ServiceOne(name: "parent"))
      }
    })

    Guice.childInjectorFactory(parentInjector)
  }


  def "parent injector objects are available"() {
    when:
    app {
      modules {
        bind ServiceTwo, new ServiceTwo(name: "child")
      }
      handlers {
        get { ServiceOne one, ServiceTwo two ->
          response.send "$one.name:$two.name"
        }
      }
    }

    then:
    text == "parent:child"
  }

}
