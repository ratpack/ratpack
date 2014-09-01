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

package ratpack.sse

import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch

import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static ratpack.sse.ServerSentEvents.serverSentEvents
import static ratpack.stream.Streams.*

class ServerSentEventsSpec extends RatpackGroovyDslSpec {

  def "can send server sent event"() {
    given:
    handlers {
      handler {
        render serverSentEvents(map(publish(1..3)) {
          new ServerSentEvent(it.toString(), "add", "Event $it".toString())
        })
      }
    }

    expect:
    def response = get()
    response.statusCode == OK.code()
    response.headers["Content-Type"] == "text/event-stream;charset=UTF-8"
    response.headers["Cache-Control"] == "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] == "no-cache"
    response.body.text == "event: add\ndata: Event 1\nid: 1\n\nevent: add\ndata: Event 2\nid: 2\n\nevent: add\ndata: Event 3\nid: 3\n\n"
  }

  def "can cancel a stream when a client drops connection"() {
    def cancelLatch = new CountDownLatch(1)
    def sentLatch = new CountDownLatch(1)

    given:
    handlers {
      handler {
        def stream = publish(1..1000)
        stream = map(stream, { new ServerSentEvent(it.toString(), "add", "Event $it".toString()) })
        stream = wiretap(stream) {
          if (it.data) {
            sentLatch.countDown()
          } else if (it.cancel) {
            cancelLatch.countDown()
          }
        }

        render serverSentEvents(stream)
      }
    }

    expect:
    URLConnection conn = getAddress().toURL().openConnection()
    conn.connect()
    InputStream is = conn.inputStream
    // wait for at least one event to be sent to the subscriber
    sentLatch.await()
    is.close()

    // when the connection is closed cancel should be called
    cancelLatch.await()
  }
}
