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

package ratpack.sse.internal

import io.netty.util.CharsetUtil
import org.reactivestreams.Publisher
import ratpack.exec.Result
import ratpack.func.Action
import ratpack.sse.Event
import ratpack.sse.ServerSentEvents
import ratpack.stream.Streams
import ratpack.stream.internal.CollectingSubscriber
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.TestByteBufAllocators

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class ServerSentEventStreamEncoderSpec extends RatpackGroovyDslSpec {

  def encoder = new ServerSentEventEncoder()

  def "can encode valid server sent events"() {
    expect:
    encoder.encode(sse, TestByteBufAllocators.LEAKING_UNPOOLED_HEAP).toString(CharsetUtil.UTF_8) == expectedEncoding

    where:
    sse                                                                                             | expectedEncoding
    serverSentEvent("foo") { it.id("${it.item}Id").event("${it.item}Type").data("${it.item}Data") } | "id: fooId\nevent: fooType\ndata: fooData\n\n"
    serverSentEvent("foo") { it.event("${it.item}Type").data("${it.item}Data") }                    | "event: fooType\ndata: fooData\n\n"
    serverSentEvent("foo") { it.data("${it.item}Data") }                                            | "data: fooData\n\n"
    serverSentEvent("foo") { it.id("${it.item}Id").data("${it.item}Data") }                         | "id: fooId\ndata: fooData\n\n"
    serverSentEvent("foo") { it.id("${it.item}Id") }                                                | "id: fooId\n\n"
    serverSentEvent("foo") { it.id("${it.item}Id").event("${it.item}Type") }                        | "id: fooId\nevent: fooType\n\n"
    serverSentEvent("foo") { it.event("${it.item}Type") }                                           | "event: fooType\n\n"
    serverSentEvent { it.id("fooId").event("fooType").data("fooData") }                             | "id: fooId\nevent: fooType\ndata: fooData\n\n"
    serverSentEvent { it.event("fooType").data("fooData") }                                         | "event: fooType\ndata: fooData\n\n"
    serverSentEvent { it.data("fooData") }                                                          | "data: fooData\n\n"
    serverSentEvent { it.id("fooId").data("fooData") }                                              | "id: fooId\ndata: fooData\n\n"
    serverSentEvent { it.id("fooId") }                                                              | "id: fooId\n\n"
    serverSentEvent { it.id("fooId").event("fooType") }                                             | "id: fooId\nevent: fooType\n\n"
    serverSentEvent { it.event("fooType") }                                                         | "event: fooType\n\n"
    serverSentEvent { it.data("foo\nbar") }                                                         | "data: foo\ndata: bar\n\n"
    serverSentEvent { it.data("foo\n") }                                                            | "data: foo\ndata: \n\n"
    serverSentEvent { it.comment("this is a \n comment").data("foo\n") }                            | ": this is a \n:  comment\ndata: foo\ndata: \n\n"
    serverSentEvent { it.comment("comment") }                                                       | ": comment\n\n"
  }

  def "errors if id contains newline"() {
    when:
    serverSentEvent { it.id("foo\nbar") }

    then:
    thrown IllegalArgumentException
  }

  def "errors if event contains newline"() {
    when:
    serverSentEvent { it.event("foo\nbar") }

    then:
    thrown IllegalArgumentException
  }

  public <T> Event serverSentEvent(T t, Action<? super Event> action) {
    toList(ServerSentEvents.serverSentEvents(Streams.publish([t]), action).publisher).get(0)
  }

  public <T> Event serverSentEvent(Action<? super Event> action) {
    serverSentEvent("foo", action)
  }

  public static <T> List<T> toList(Publisher<T> publisher) throws Exception {
    CountDownLatch latch = new CountDownLatch(1)
    AtomicReference<Result<List<T>>> ref = new AtomicReference<>()

    Thread.start {
      publisher.subscribe(new CollectingSubscriber<T>({
        ref.set(it)
        latch.countDown()
      }, { it.request(Long.MAX_VALUE) }))
    }

    latch.await()
    ref.get().valueOrThrow
  }

}
