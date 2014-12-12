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

package ratpack.logging

import com.google.inject.Binder
import com.google.inject.Injector
import ratpack.guice.HandlerDecoratingModule
import ratpack.handling.Handler
import ratpack.http.client.ReceivedResponse
import ratpack.test.internal.RatpackGroovyDslSpec

class LoggingSpec extends RatpackGroovyDslSpec {

  private class TestLoggingModule implements HandlerDecoratingModule {
    public Handler decorate(Injector injector, Handler handler) {
      return new CorrelationIdHandler(new RequestLoggingHandler(handler))
    }

    @Override
    void configure(Binder binder) {

    }
  }

  def "add request uuids"() {
    given: 'a ratpack app with the logging request handlers added'
    bindings {
      add new TestLoggingModule()
    }

    handlers {
      handler {
        render request.get(RequestCorrelationId).id
      }
    }

    when: 'a response is received'
    ReceivedResponse response = get()

    then: 'a correlation id was returned'
    response.body.text.length() == 36 // not the best test ever but UUIDs should be 36 characters long including the dashes.
  }
}
