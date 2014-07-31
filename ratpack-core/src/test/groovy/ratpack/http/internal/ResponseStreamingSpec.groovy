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

package ratpack.http.internal

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.http.ServerSentEvent
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch

import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static ratpack.http.HttpResponseChunks.httpResponseChunks
import static ratpack.http.ServerSentEvents.serverSentEvents

class ResponseStreamingSpec extends RatpackGroovyDslSpec {

  def "can send chunked response"() {
    given:
    handlers {
      handler {
        render httpResponseChunks(new LargeContentPublisher())
      }
    }

    expect:
    def chunkedResponse = []
    Socket socket = new Socket(getAddress().host, getAddress().port)
    try {
      new OutputStreamWriter(socket.outputStream, "UTF-8").with {
        write("GET / HTTP/1.1\r\n")
        write("\r\n")
        flush()
      }

      InputStreamReader inputStreamReader = new InputStreamReader(socket.inputStream)
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader)

      chunkedResponse << bufferedReader.readLine()
      def chunk
      while ((chunk = bufferedReader.readLine()) != null) {
        chunkedResponse << chunk
      }
    } finally {
      socket.close()
    }

    chunkedResponse == ['HTTP/1.1 200 OK', 'Transfer-Encoding: chunked', 'Connection: keep-alive', '', '14', 'This is a really lon', '14', 'g string that needs ', '12', 'to be sent chunked', '0', '']
  }

  def "can send server sent event"() {
    given:
    handlers {
      handler {
        render serverSentEvents(new SseStreamer())
      }
    }

    expect:
    def response = get()
    response.statusCode == OK.code()
    response.header("Content-Type") == "text/event-stream;charset=UTF-8"
    response.header("Cache-Control") == "no-cache, no-store, max-age=0, must-revalidate"
    response.header("Pragma") == "no-cache"
    response.body.asString() == "event: add\ndata: Event 1\nid: 1\n\nevent: add\ndata: Event 2\nid: 2\n\nevent: add\ndata: Event 3\nid: 3\n\n"
  }

  def "can cancel a stream when a client drops connection"() {
    def cancelLatch = new CountDownLatch(1)
    def onNextLatch = new CountDownLatch(1)

    given:
    handlers {
      handler {
        render serverSentEvents(new Publisher<ServerSentEvent>() {
          @Override
          void subscribe(Subscriber<ServerSentEvent> s) {
            s.onSubscribe(new Subscription() {

              @Override
              void request(int n) {
                Thread.start {
                  (0..100).each {
                    s.onNext(new ServerSentEvent(it.toString(), "add", "Event $it".toString()))
                    onNextLatch.countDown()
                    Thread.sleep(100)
                  }

                  s.onComplete()
                }
              }

              @Override
              void cancel() {
                cancelLatch.countDown()
              }
            })
          }
        })
      }
    }

    expect:
    URLConnection conn = getAddress().toURL().openConnection()
    conn.connect()
    InputStream is = conn.inputStream
    // wait for at least one event to be sent to the subscriber
    onNextLatch.await()
    is.close()

    // when the connection is closed Subsctiption#cancel should be called
    cancelLatch.await()
  }
}
