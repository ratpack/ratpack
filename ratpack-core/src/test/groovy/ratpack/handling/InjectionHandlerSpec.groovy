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
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.error.internal.DefaultProductionErrorHandler
import ratpack.file.FileSystemBinding
import ratpack.file.internal.DefaultFileSystemBinding
import ratpack.test.internal.RatpackGroovyDslSpec

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
      all new InjectedHandler()
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

  static class InjectedOptionalHandler1 extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context context, Optional<FileSystemBinding> fileSystemBinding) {
      assert fileSystemBinding.get().is(context.get(FileSystemBinding))
      context.render("ok")
    }
  }

  static class InjectedOptionalHandler2 extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context context, Optional<Exception> exceptionOptional) {
      assert !exceptionOptional.isPresent()
      context.render("ok")
    }
  }

  def "can inject more than one"() {
    when:
    handlers {
      all new Injected2Handler()
    }

    then:
    text == DefaultProductionErrorHandler.name
  }

  def "can inject optional"() {
    when:
    handlers {
      all new InjectedOptionalHandler1()
    }

    then:
    text == "ok"
  }

  def "can inject missing"() {
    when:
    handlers {
      all new InjectedOptionalHandler2()
    }

    then:
    text == "ok"
  }

  static class InjectedBadHandler extends InjectionHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding, Exception notInRegistry) {}
  }

  def "error when cant inject"() {
    when:
    handlers {
      register {
        add ServerErrorHandler, new DefaultDevelopmentErrorHandler()
      }
      all new InjectedBadHandler()
    }

    then:
    text =~ "No object for type 'java\\.lang\\.Exception' in registry"
  }

  def "can inject from request registry"() {
    when:
    bindings {
      bindInstance 10
    }
    handlers {
      all {
        request.add("foo")
        next()
      }
      all new InjectedPrimitivesHandler()
    }

    then:
    text == "10:foo"
  }

  def "request registry shadows context registry"() {
    when:
    bindings {
      bindInstance 10
      bindInstance "bar"
    }
    handlers {
      all {
        request.add("foo")
        next()
      }
      all new InjectedPrimitivesHandler()
    }

    then:
    text == "10:foo"
  }

  def "injection handler accessibility #injectionHandler.class"() {
    when:
    handlers {
      all injectionHandler
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
      all new GenericInjectionHandler()
    }

    then:
    text == "foo:bar"
  }

}
