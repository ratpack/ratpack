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

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import ratpack.http.client.BaseHttpClientSpec
import ratpack.stream.TransformablePublisher

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.zip.GZIPInputStream

import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.sse.ServerSentEvents.serverSentEvents
import static ratpack.stream.Streams.*

class ServerSentEventsSpec extends BaseHttpClientSpec {

  def "can send server sent event"() {
    given:
    handlers {
      all {
        render serverSentEvents(publish(1..3)) { Event event ->
          event.id(event.item.toString()).event("add").data("Event ${event.item}".toString())
        }
      }
    }

    expect:
    def response = get()
    response.body.text == """id: 1
event: add
data: Event 1

id: 2
event: add
data: Event 2

id: 3
event: add
data: Event 3

"""
    response.statusCode == OK.code()
    response.headers["Content-Type"] == "text/event-stream;charset=UTF-8"
    response.headers["Cache-Control"] == "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] == "no-cache"
    response.headers["Content-Encoding"] == null
  }

  def "can cancel a stream when a client drops connection"() {
    def cancelLatch = new CountDownLatch(1)
    def sentLatch = new CountDownLatch(1)

    given:
    handlers {
      all {
        def stream = publish(1..1000)
        stream = wiretap(stream) {
          if (it.data) {
            sentLatch.countDown()
          } else if (it.cancel) {
            cancelLatch.countDown()
          }
        }

        render serverSentEvents(stream) {
          it.id({ it.toString() }).event("add").data({ "Event ${it}".toString() })
        }
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

  def "can cancel a stream after netty channel is closed"() {
    def sentLatch = new CountDownLatch(1)
    def clientClosedLatch = new CountDownLatch(1)
    def channelClosedLatch = new CountDownLatch(1)
    def cancelLatch = new CountDownLatch(1)

    given:
    handlers {
      all {
        context.directChannelAccess.channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          void operationComplete(Future<? super Void> future) throws Exception {
            channelClosedLatch.countDown()
          }
        })

        def stream = publish(1..1000).wiretap {
          if (it.data) {
            sentLatch.countDown()

            // the client disconnecting doesn't close the netty channel for some reason, forcibly close it
            clientClosedLatch.await()
            context.directChannelAccess.channel.close()

            // wait for the channel to close before letting the element through to the subscriber
            channelClosedLatch.await()
          } else if (it.cancel) {
            cancelLatch.countDown()
          }
        }

        render serverSentEvents(stream) {
          it.id({ it.toString() }).event("add").data({ "Event ${it}".toString() })
        }
      }
    }

    expect:
    URLConnection conn = getAddress().toURL().openConnection()
    conn.connect()
    InputStream is = conn.inputStream

    // wait for at least one event to be sent to the subscriber
    sentLatch.await()
    is.close()
    clientClosedLatch.countDown()

    // when the channel is closed, cancel should be called
    cancelLatch.await()
  }

  def "can send compressed server sent event"() {
    given:
    requestSpec {
      it.headers.add("Accept-Encoding", "gzip")
      it.decompressResponse(false)
    }

    and:
    handlers {
      all {
        render serverSentEvents(publish(1..3)) {
          switch (it.item) {
            case 1:
            case 2:
              it.id(it.item.toString()).event("add").data("Event ${it.item}".toString())
              break
            case 3:
              it.comment("last")
          }
        }
      }
    }

    expect:
    def response = get()
    response.statusCode == OK.code()
    response.headers["Content-Type"] == "text/event-stream;charset=UTF-8"
    response.headers["Cache-Control"] == "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] == "no-cache"
    response.headers["Content-Encoding"] == "gzip"

    new GZIPInputStream(response.body.inputStream).text ==
      "id: 1\nevent: add\ndata: Event 1\n\nid: 2\nevent: add\ndata: Event 2\n\n: last\n\n"
  }

  def "can consume server sent event stream"() {
    given:
    otherApp {
      get("foo") {
        def stream = periodically(context.execution.controller.executor, Duration.ofMillis(100)) { it < 10 ? it : null }

        render serverSentEvents(stream) { Event<Integer> event ->
          event.id(event.item.toString()).data("Event ${event.item}".toString())
        }
      }
    }

    and:
    handlers {
      get { ServerSentEventStreamClient sseStreamClient ->
        sseStreamClient.request(otherAppUrl("foo")).then { TransformablePublisher<Event<?>> eventStream ->
          render stringChunks(
            eventStream.map { it.data }
          )
        }
      }
    }

    expect:
    getText() == "Event 0Event 1Event 2Event 3Event 4Event 5Event 6Event 7Event 8Event 9"
  }

}
