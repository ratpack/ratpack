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
import com.google.inject.CreationException
import com.google.inject.Injector
import com.google.inject.Module
import ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchException
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.func.Transformer

import static com.google.inject.Guice.createInjector

class GuiceParentInjectorSpec extends RatpackGroovyDslSpec {

  static class ServiceOne {
    String name
  }

  static class ServiceTwo {
    String name
  }

  @Override
  ClosureBackedEmbeddedApplication createApplication() {
    return new ClosureBackedEmbeddedApplication({ baseDir.build() }) {
      @Override
      protected Transformer<? super Module, ? extends Injector> createInjectorFactory(LaunchConfig launchConfig) {
        Injector parentInjector = createInjector(new AbstractModule() {
          @Override
          protected void configure() {
            bind(ServiceOne).toInstance(new ServiceOne(name: "parent"))
          }
        })

        Guice.childInjectorFactory(parentInjector)
      }
    }
  }

  def "objects from parent are available"() {
    when:
    bindings {
      bind ServiceTwo, new ServiceTwo(name: "child")
    }
    handlers {
      get { ServiceOne one, ServiceTwo two ->
        response.send "$one.name:$two.name"
      }
    }

    then:
    text == "parent:child"
  }

  def "fails to override parent binding"() {
    when:
    bindings {
      bind ServiceOne, new ServiceOne(name: "child")
    }
    handlers {
      get { ServiceOne one ->
        response.send "$one.name"
      }
    }

    server.start()

    then:
    def e = thrown LaunchException
    e.cause instanceof CreationException
    def creationException = e.cause as CreationException
    creationException.errorMessages.first().message.contains "A binding to $ServiceOne.name was already configured"
  }

}
