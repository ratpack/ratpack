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
import ratpack.exec.Promise
import ratpack.http.Status
import ratpack.http.client.BaseHttpClientSpec

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static ratpack.stream.Streams.*

class ServerSentEventsSpec extends BaseHttpClientSpec {

  def "can send server sent event"() {
    given:
    handlers {
      all {
        render ServerSentEvents.builder().build(publish(1..3).map {
          ServerSentEvent.builder().id(it.toString()).event("add").data("Event ${it}".toString()).build()
        })
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

        render ServerSentEvents.builder().build(stream.map {
          ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
        })
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
          if (it.cancel) {
            cancelLatch.countDown()
          }
        }

        render ServerSentEvents.builder().build(stream.map {
          if (it == 200) {
            sentLatch.countDown()
            clientClosedLatch.await()
          }
          ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
        })
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
        render ServerSentEvents.builder().build(publish(1..3).map {
          switch (it) {
            case 1:
            case 2:
              return ServerSentEvent.builder().id(it.toString()).event("add").data("Event ${it}".toString()).build()
            case 3:
              return ServerSentEvent.builder().comment("last").build()
          }
        })
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

  def "can send empty stream"() {
    given:
    handlers {
      all {
        render ServerSentEvents.builder().build(publish([] as List<ServerSentEvent>))
      }
    }

    expect:
    def response = get()
    response.body.text == ""
    response.statusCode == OK.code()
    response.headers["Content-Type"] == "text/event-stream;charset=UTF-8"
    response.headers["Cache-Control"] == "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] == "no-cache"
    response.headers["Content-Encoding"] == null
  }

  def "can send no content stream"() {
    given:
    handlers {
      all {
        render ServerSentEvents.builder().noContentOnEmpty().build(publish([] as List<ServerSentEvent>))
      }
    }

    expect:
    def response = get()
    response.body.text == ""
    response.status == Status.NO_CONTENT
    response.headers["Cache-Control"] == "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] == "no-cache"
  }

  def "can send stream when stream is async and using no content on empty"() {
    given:
    handlers {
      all {
        def stream = flatYield { req ->
          Promise.async { down ->
            execution.eventLoop.schedule({ down.success(req.requestNum < 5 ? req.requestNum : null) }, 20, TimeUnit.MILLISECONDS)
          }
        }.bindExec().gate {
          execution.eventLoop.schedule(it, 20, TimeUnit.MILLISECONDS)
        }
        render ServerSentEvents.builder().noContentOnEmpty().build(stream.map {
          ServerSentEvent.builder().id(it.toString()).event("add").data("Event ${it}".toString()).build()
        })
      }
    }

    expect:
    def response = get()
    response.body.text == """
id: 0
event: add
data: Event 0

id: 1
event: add
data: Event 1

id: 2
event: add
data: Event 2

id: 3
event: add
data: Event 3

id: 4
event: add
data: Event 4

""".stripLeading()
  }

  def "can send no content stream when stream is async and using no content on empty"() {
    given:
    handlers {
      all {
        def stream = flatYield { req ->
          Promise.async { down ->
            execution.eventLoop.schedule({ down.success(null) }, 100, TimeUnit.MILLISECONDS)
          }
        }.bindExec().gate {
          execution.eventLoop.schedule(it, 20, TimeUnit.MILLISECONDS)
        }
        render ServerSentEvents.builder().noContentOnEmpty().build(stream.map {
          ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
        })
      }
    }

    expect:
    def response = get()
    response.body.text == ""
    response.status == Status.NO_CONTENT
  }

  def "can send heartbeats to keep stream alive"() {
    given:
    handlers {
      all {
        def stream = flatYield { req ->
          Promise.async { down ->
            execution.eventLoop.schedule({ down.success(req.requestNum > 1 ? null : req.requestNum) }, 100, TimeUnit.MILLISECONDS)
          }
        }
        render ServerSentEvents.builder()
            .keepAlive(Duration.ofMillis(10))
            .build(stream.map {
              ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
            })
      }
    }

    expect:
    def response = get()
    def text = response.body.text

    text.contains("""
: keepalive heartbeat

id: 0
data: Event 0

: keepalive heartbeat
""")

    text.contains("""
: keepalive heartbeat

id: 1
data: Event 1

: keepalive heartbeat
""")
  }

  def "can send heartbeats to keep stream alive when compressed"() {
    given:
    handlers {
      all {
        def stream = flatYield { req ->
          Promise.async { down ->
            execution.eventLoop.schedule({ down.success(req.requestNum > 1 ? null : req.requestNum) }, 100, TimeUnit.MILLISECONDS)
          }
        }
        render ServerSentEvents.builder()
            .keepAlive(Duration.ofMillis(10))
            .build(stream.map {
              ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
            })
      }
    }

    expect:
    def response = request { it.headers.add("Accept-Encoding", "gzip") }
    def text = response.body.text

    text.contains("""
: keepalive heartbeat

id: 0
data: Event 0

: keepalive heartbeat
""")

    text.contains("""
: keepalive heartbeat

id: 1
data: Event 1

: keepalive heartbeat
""")
  }

  def "can buffer with heart beats"() {
    given:
    handlers {
      all {
        def stream = flatYield { req ->
          Promise.async { down ->
            execution.eventLoop.schedule({ down.success(req.requestNum > 1 ? null : req.requestNum) }, 1, TimeUnit.SECONDS)
          }
        }
        render ServerSentEvents.builder()
            .keepAlive(Duration.ofMillis(600))
            .buffered(30, Duration.ofMillis(4000))
            .build(stream.map {
              ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
            })
      }
    }

    expect:
    def response = get()
    def text = response.body.text

    text == """: keepalive heartbeat

: keepalive heartbeat

: keepalive heartbeat

id: 0
data: Event 0

id: 1
data: Event 1

: keepalive heartbeat

"""

  }

}
