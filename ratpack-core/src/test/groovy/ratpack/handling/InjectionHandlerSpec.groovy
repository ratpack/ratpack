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

package ratpack.handling

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import ratpack.error.DebugErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultServerErrorHandler
import ratpack.file.FileSystemBinding
import ratpack.file.internal.DefaultFileSystemBinding
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

class InjectionHandlerSpec extends RatpackGroovyDslSpec {

  static class NoHandleMethod extends InjectionHandler {}

  def "must have handle method"() {
    when:
    new NoHandleMethod()

    then:
    thrown InjectionHandler.NoSuitableHandleMethodException
  }

  static class NoHandleMethodWithContextAsFirstParam extends InjectionHandler {
    void handle(String foo) {}
  }

  def "must have handle method with context as first param"() {
    when:
    new NoHandleMethodWithContextAsFirstParam()

    then:
    thrown InjectionHandler.NoSuitableHandleMethodException
  }

  static class InjectedHandler extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding) {
      assert fileSystemBinding.is(exchange.get(FileSystemBinding))
      exchange.response.send(fileSystemBinding.class.name)
    }
  }

  def "can inject"() {
    when:
    handlers {
      handler new InjectedHandler()
    }

    then:
    text == DefaultFileSystemBinding.name
  }

  static class Injected2Handler extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding, ServerErrorHandler serverErrorHandler) {
      assert fileSystemBinding.is(exchange.get(FileSystemBinding))
      assert serverErrorHandler.is(exchange.get(ServerErrorHandler))
      exchange.response.send(serverErrorHandler.class.name)
    }
  }

  static class InjectedPrimitivesHandler extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context context, Integer integer, String string) {
      context.render("$integer:$string")
    }
  }


  def "can inject more than one"() {
    when:
    handlers {
      handler new Injected2Handler()
    }

    then:
    text == DefaultServerErrorHandler.name
  }

  static class InjectedBadHandler extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding, Exception notInRegistry) {}
  }

  def "error when cant inject"() {
    when:
    handlers {
      register {
        add ServerErrorHandler, new DebugErrorHandler()
      }
      handler new InjectedBadHandler()
    }

    then:
    text =~ "No object for type 'java\\.lang\\.Exception' in registry"
  }

  def "can inject from request registry"() {
    when:
    modules {
      bind 10
    }
    handlers {
      handler {
        request.register("foo")
        next()
      }
      handler new InjectedPrimitivesHandler()
    }

    then:
    text == "10:foo"
  }

  def "context registry shadows request registry"() {
    when:
    modules {
      bind 10
      bind "bar"
    }
    handlers {
      handler {
        request.register("foo")
        next()
      }
      handler new InjectedPrimitivesHandler()
    }

    then:
    text == "10:bar"
  }

  @Unroll
  def "injection handler accessibility #injectionHandler.class"() {
    when:
    handlers {
      handler injectionHandler
    }

    then:
    text == "ok"

    where:
    injectionHandler << [
      new TestInjectionHandlers.MethodIsProtected(),
      new TestInjectionHandlers.MethodIsPrivate(),
      new TestInjectionHandlers().publicInnerWithPrivate(),
      new TestInjectionHandlers().privateInnerWithPrivate()
    ]
  }

  static class GenericInjectionHandler extends InjectionHandler {
    protected handle(Context context, List<String> strings) {
      context.render(strings.join(":"))
    }
  }

  def "can inject generic type"() {
    when:
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<List<String>>() {}).toInstance(["foo", "bar"])
      }
    }

    and:
    handlers {
      handler new GenericInjectionHandler()
    }

    then:
    text == "foo:bar"
  }

}
