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

package ratpack.sse.client

import ratpack.exec.Promise
import ratpack.http.client.BaseHttpClientSpec
import ratpack.sse.ServerSentEvent
import ratpack.sse.ServerSentEvents
import ratpack.stream.Streams

import java.time.Duration
import java.util.concurrent.TimeUnit

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.stream.Streams.flatYield
import static ratpack.stream.Streams.periodically

class ServerSentEventClientSpec extends BaseHttpClientSpec {

  def "can consume server sent event stream"() {
    given:
    otherApp {
      get("foo") {
        def stream = periodically(context.execution.controller.executor, Duration.ofMillis(100)) { it < 10 ? it : null }

        render ServerSentEvents.builder().build(stream.map {
          ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
        })
      }
    }

    and:
    handlers {
      get { ServerSentEventClient sseClient ->
        sseClient.request(otherAppUrl("foo")).then { eventStream ->
          render stringChunks(
            eventStream.events.map { it.data }
          )
        }
      }
    }

    expect:
    getText() == "Event 0Event 1Event 2Event 3Event 4Event 5Event 6Event 7Event 8Event 9"
  }

  def "can consume empty stream"() {
    given:
    otherApp {
      get {
        render ServerSentEvents.builder().build(Streams.empty())
      }
    }

    and:
    handlers {
      get { ServerSentEventClient sseClient ->
        sseClient.request(otherAppUrl()).then { eventStream ->
          render stringChunks(
            eventStream.events.map { it.data }
          )
        }
      }
    }

    expect:
    getText() == ""
  }

  def "can receive no content stream"() {
    given:
    otherApp {
      get {
        render ServerSentEvents.builder().noContentOnEmpty().build(Streams.empty())
      }
    }

    and:
    handlers {
      get { ServerSentEventClient sseClient ->
        sseClient.request(otherAppUrl()).then { eventStream ->
          render stringChunks(
            eventStream.events.map { it.data }
          )
        }
      }
    }

    expect:
    getText() == ""
  }

  def "can consume heartbeats to keep stream alive"() {
    given:
    otherApp {
      all {
        def stream = flatYield { req ->
          Promise.async { down ->
            execution.eventLoop.schedule({ down.success(req.requestNum > 1 ? null : req.requestNum) }, 1, TimeUnit.SECONDS)
          }
        }
        render ServerSentEvents.builder()
          .keepAlive(Duration.ofMillis(10))
          .build(stream.map {
            ServerSentEvent.builder().id(it.toString()).data("Event ${it}".toString()).build()
          })
      }
    }

    and:
    handlers {
      get { ServerSentEventClient sseClient ->
        sseClient.request(otherAppUrl(), { it.readTimeout(Duration.ofMillis(500)) }).then { eventStream ->
          render stringChunks(
            eventStream.events.map { it.data }
          )
        }
      }
    }

    expect:
    text == "Event 0Event 1"
  }

}
