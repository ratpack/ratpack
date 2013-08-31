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

package org.ratpackframework.handling

import org.ratpackframework.error.ServerErrorHandler
import org.ratpackframework.error.internal.DefaultServerErrorHandler
import org.ratpackframework.file.FileSystemBinding
import org.ratpackframework.file.internal.DefaultFileSystemBinding
import org.ratpackframework.test.internal.DefaultRatpackSpec

import static java.util.Collections.singletonList
import static org.ratpackframework.handling.Handlers.register

class ServiceUsingHandlerSpec extends DefaultRatpackSpec {

  static class NoHandleMethod extends ServiceUsingHandler {}

  def "must have handle method"() {
    when:
    new NoHandleMethod()

    then:
    thrown ServiceUsingHandler.NoSuitableHandleMethodException
  }

  static class NoHandleMethodWithContextAsFirstParam extends ServiceUsingHandler {
    void handle(String foo) {}
  }

  def "must have handle method with context as first param"() {
    when:
    new NoHandleMethodWithContextAsFirstParam()

    then:
    thrown ServiceUsingHandler.NoSuitableHandleMethodException
  }

  static class InjectedHandler extends ServiceUsingHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding) {
      assert fileSystemBinding.is(exchange.get(FileSystemBinding))
      exchange.response.send(fileSystemBinding.class.name)
    }
  }

  def "can inject"() {
    when:
    app {
      handlers {
        handler new InjectedHandler()
      }
    }

    then:
    text == DefaultFileSystemBinding.class.name
  }

  static class Injected2Handler extends ServiceUsingHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding, ServerErrorHandler serverErrorHandler) {
      assert fileSystemBinding.is(exchange.get(FileSystemBinding))
      assert serverErrorHandler.is(exchange.get(ServerErrorHandler))
      exchange.response.send(serverErrorHandler.class.name)
    }
  }

  def "can inject more than one"() {
    when:
    app {
      handlers {
        handler new Injected2Handler()
      }
    }

    then:
    text == DefaultServerErrorHandler.class.name
  }

  static class InjectedBadHandler extends ServiceUsingHandler {
    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    protected handle(Context exchange, FileSystemBinding fileSystemBinding, Exception notInRegistry) {}
  }

  static class MessageServerErrorHandler implements ServerErrorHandler {
    void error(Context exchange, Exception exception) {
      exchange.response.status(500).send(exception.message)
    }
  }

  def "error when cant inject"() {
    when:
    app {
      handlers {
        handler register(ServerErrorHandler, new MessageServerErrorHandler(), singletonList(new InjectedBadHandler()))
      }
    }

    then:
    text =~ "No object for type 'java.lang.Exception' in registry"
  }
}
