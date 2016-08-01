/*
 * Copyright 2015 the original author or authors.
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

import ratpack.func.Action
import ratpack.groovy.handling.GroovyChain
import ratpack.registry.Registry
import ratpack.test.internal.RatpackGroovyDslSpec

class HandlerDecorationSpec extends RatpackGroovyDslSpec {

  def "decorators are applied FIFO"() {
    given:
    def events = []

    when:
    bindings {
      multiBindInstance HandlerDecorator.prepend( { events << "1"; it.next() } as Handler)
      multiBindInstance HandlerDecorator.prepend( { events << "2"; it.next() } as Handler)
    }
    handlers {
      get { render "ok" }
    }

    then:
    text == "ok"
    events == ["1", "2"]
  }


  def "decorators are applied FIFO when specified by class"() {
    when:
    bindings {
      bind(MyHandler)
      multiBindInstance HandlerDecorator.prepend(MyHandler)
    }
    handlers {
      get { render get(String) }
    }

    then:
    text == "foo"
  }

  def "invoke decorator once"() {
    when:
    bindings {
      multiBind InjectedDecoratorHandler
    }
    handlers {
      get { render "ok" }
    }

    then:
    text == "ok"
  }

  def "handler chain is applied and work with existing handlers"() {
    given:
    bindings {
      bind(MyChain)
      multiBindInstance(HandlerDecorator.prependHandlers(MyChain))
    }
    handlers {
      get { render "ok" }
    }

    when:
    get('')

    then:
    response.body.text == "ok"

    when:
    get('prepended')

    then:
    response.body.text == "prepended"
  }

  def "handler chain is applied and work with no other handlers installed"() {
    given:
    bindings {
      bind(MyChain)
      multiBindInstance(HandlerDecorator.prependHandlers(MyChain))
    }

    when:
    get("prepended")

    then:
    response.body.text == "prepended"
  }

  def "prepended handler instance in a chain matches path and calls next"() {
    given:
    Action<? extends GroovyChain> chain = {
      it.all { c -> c.next(c.single("added")) }
    }
    bindings {
      bindInstance(chain)
      multiBindInstance(HandlerDecorator.prependHandlers(chain))
    }
    handlers {
      get("prepended") { render it.get(String) }
    }

    when:
    get("prepended")

    then:
    response.body.text == "added"
  }

  def "prepended handler class in a chain matches path and calls next"() {
    given:
    bindings {
      bind(MyChain2)
      multiBindInstance(HandlerDecorator.prependHandlers(MyChain2))
    }
    handlers {
      get("prepended") {
        render it.get(String)
      }
    }

    when:
    get("prepended")

    then:
    response.body.text == "added"
  }

  static class InjectedDecoratorHandler implements HandlerDecorator {
    static int invocations = 0

    @Override
    Handler decorate(Registry serverRegistry, Handler rest) throws Exception {
      invocations++

      assert invocations == 1
      return { ctx ->
        ctx.insert(rest)
      }
    }
  }

  static class MyHandler implements Handler {

    @Override
    void handle(Context ctx) throws Exception {
      ctx.next(Registry.single("foo"))
    }
  }

  static class MyChain implements Action<Chain> {

    @Override
    void execute(Chain chain) throws Exception {
      chain.prefix("prepended") { chain1 ->
        chain1.get { ctx ->
          ctx.render "prepended"
        }
      }
    }
  }
  static class MyChain2 implements Action<Chain> {

    @Override
    void execute(Chain chain) throws Exception {
      chain.all { ctx ->
        ctx.next(ctx.single("added"))
      }
    }
  }


}
