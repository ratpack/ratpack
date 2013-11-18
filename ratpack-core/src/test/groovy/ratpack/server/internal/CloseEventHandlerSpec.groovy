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

package ratpack.server.internal

import ratpack.handling.internal.ContextClose
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.Action

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class CloseEventHandlerSpec extends RatpackGroovyDslSpec {

  def "close on successful request"() {
    def latch = new CountDownLatch(2)
    def status

    given:
    app {
      handlers {

        handler {
          onClose(new Action<ContextClose>() {
            @Override
            void execute(ContextClose eventContext) {
              status = eventContext.status
              latch.countDown()
            }
          })

          next()
        }

        handler("foo") {
          latch.countDown()
          render ""
        }

      }
    }

    when:
    getText("foo")
    latch.await(2, TimeUnit.SECONDS)

    then:
    latch.count == 0
    status.code == OK.code()
  }

  def "close on unsuccessful request"() {
    def latch = new CountDownLatch(1)
    def status

    given:
    app {
      handlers {

        handler {
          onClose(new Action<ContextClose>() {
            @Override
            void execute(ContextClose eventContext) {
              status = eventContext.status
              latch.countDown()
            }
          })

          next()
        }

        handler("foo") {
          render ""
        }

      }
    }

    when:
    getText("bar")
    latch.await(2, TimeUnit.SECONDS)

    then:
    latch.count == 0
    status.code == NOT_FOUND.code()
  }

  def "on close with multiple event handler"() {
    def latch = new CountDownLatch(3)
    def events = []

    given:
    app {
      handlers {

        handler {
          onClose(new Action<ContextClose>() {
            @Override
            void execute(ContextClose eventContext) {
              events << "event1"
              latch.countDown()
            }
          })

          onClose(new Action<ContextClose>() {
            @Override
            void execute(ContextClose eventContext) {
              events << "event2"
              latch.countDown()
            }
          })

          next()
        }

        handler("foo") {
          latch.countDown()
          render ""
        }

      }
    }

    when:
    getText("foo")
    latch.await(2, TimeUnit.SECONDS)

    then:
    latch.count == 0
    // The onClose executions are off the main thread but all onClose executions happen
    // on the same background thread.  Therefore order should be guaranteed.
    events == ["event1", "event2"]
  }
}
