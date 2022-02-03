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

package ratpack.core.sse.internal

import io.netty.buffer.ByteBufAllocator
import io.netty.util.CharsetUtil
import ratpack.core.sse.ServerSentEvent
import ratpack.core.sse.ServerSentEventBuilder
import ratpack.test.internal.RatpackGroovyDslSpec

class ServerSentEventStreamEncoderSpec extends RatpackGroovyDslSpec {

  def encoder = new ServerSentEventEncoder()

  def "can encode valid server sent events"() {
    expect:
    def byteBuf = encoder.encode(sse, ByteBufAllocator.DEFAULT)
    def string = byteBuf.toString(CharsetUtil.UTF_8)
    byteBuf.release()
    string == expectedEncoding

    where:
    sse                                                                  | expectedEncoding
    serverSentEvent { it.id("fooId").event("fooType").data("fooData") }  | "id: fooId\nevent: fooType\ndata: fooData\n\n"
    serverSentEvent { it.event("fooType").data("fooData") }              | "event: fooType\ndata: fooData\n\n"
    serverSentEvent { it.data("fooData") }                               | "data: fooData\n\n"
    serverSentEvent { it.id("fooId").data("fooData") }                   | "id: fooId\ndata: fooData\n\n"
    serverSentEvent { it.id("fooId") }                                   | "id: fooId\n\n"
    serverSentEvent { it.id("fooId").event("fooType") }                  | "id: fooId\nevent: fooType\n\n"
    serverSentEvent { it.event("fooType") }                              | "event: fooType\n\n"
    serverSentEvent { it.id("fooId").event("fooType").data("fooData") }  | "id: fooId\nevent: fooType\ndata: fooData\n\n"
    serverSentEvent { it.event("fooType").data("fooData") }              | "event: fooType\ndata: fooData\n\n"
    serverSentEvent { it.data("fooData") }                               | "data: fooData\n\n"
    serverSentEvent { it.id("fooId").data("fooData") }                   | "id: fooId\ndata: fooData\n\n"
    serverSentEvent { it.id("fooId") }                                   | "id: fooId\n\n"
    serverSentEvent { it.id("fooId").event("fooType") }                  | "id: fooId\nevent: fooType\n\n"
    serverSentEvent { it.event("fooType") }                              | "event: fooType\n\n"
    serverSentEvent { it.data("foo\nbar") }                              | "data: foo\ndata: bar\n\n"
    serverSentEvent { it.data("foo\n") }                                 | "data: foo\ndata: \n\n"
    serverSentEvent { it.comment("this is a \n comment").data("foo\n") } | ": this is a \n:  comment\ndata: foo\ndata: \n\n"
    serverSentEvent { it.comment("comment") }                            | ": comment\n\n"
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

  ServerSentEvent serverSentEvent(@DelegatesTo(ServerSentEventBuilder) Closure<?> cl) {
    ServerSentEvent.builder().tap(cl as Closure<Object>).build()
  }

}
