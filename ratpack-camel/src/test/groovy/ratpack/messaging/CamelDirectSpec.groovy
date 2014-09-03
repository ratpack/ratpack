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

package ratpack.messaging

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Scopes
import org.apache.camel.CamelContext
import org.apache.camel.impl.DefaultCamelContext
import ratpack.messaging.internal.Camel
import spock.lang.Specification
import spock.lang.Subject

class CamelDirectSpec extends Specification {

  Injector injector = getInjector()
  @Subject camel = new Camel(injector, injector.getInstance(CamelContext))
  CamelContext context

  def setup() {
    context = injector.getInstance(CamelContext)
    camel.init()
  }

  void "handler should receive messages"() {
    setup:
    def handler = injector.getInstance(TestMessageHandler)

    when:
    context.createProducerTemplate().sendBody("direct:handler", "foo")

    then:
    handler.received
  }

  void "many handlers should chain together"() {
    setup:
    def handler = injector.getInstance(TestMessageHandler)
    def handler2 = injector.getInstance(TestMessageHandler2)

    when:
    context.createProducerTemplate().sendBody("direct:handler", "foo")

    then:
    handler.received
    handler2.received
  }

  def getInjector() {
    Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(CamelContext).toInstance(new DefaultCamelContext())
        bind(TestMessageHandler).in(Scopes.SINGLETON)
        bind(TestMessageHandler2).in(Scopes.SINGLETON)
      }
    })
  }

  static class AbstractTestMessageHandler implements MessageHandler {
    String url = "direct:handler"

    boolean received

    @Override
    void handle(MessageContext context) {
      assert context.request.message == "foo"
      received = true
      context.next()
    }
  }

  static class TestMessageHandler extends AbstractTestMessageHandler {
  }

  static class TestMessageHandler2 extends AbstractTestMessageHandler {
  }
}
