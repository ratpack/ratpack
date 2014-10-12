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

import io.netty.buffer.UnpooledByteBufAllocator
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.internal.IoUtils

import static ratpack.sse.ServerSentEvent.serverSentEvent

class ServerSentEventStreamEncoderSpec extends RatpackGroovyDslSpec {

  def encoder = new ServerSentEventsRenderer.Encoder(UnpooledByteBufAllocator.DEFAULT)

  def "can encode valid server sent events"() {
    expect:
    IoUtils.utf8String(encoder.apply(sse)) == expectedEncoding

    where:
    sse                                                                                             | expectedEncoding
    serverSentEvent("foo") { it.id("${it.item}Id").event("${it.item}Type").data("${it.item}Data") } | "event: fooType\ndata: fooData\nid: fooId\n\n"
    serverSentEvent("foo") { it.event("${it.item}Type").data("${it.item}Data") }                    | "event: fooType\ndata: fooData\n\n"
    serverSentEvent("foo") { it.data("${it.item}Data") }                                            | "data: fooData\n\n"
    serverSentEvent("foo") { it.id("${it.item}Id").data("${it.item}Data") }                         | "data: fooData\nid: fooId\n\n"
    serverSentEvent("foo") { it.id("${it.item}Id") }                                                | "id: fooId\n\n"
    serverSentEvent("foo") { it.id("${it.item}Id").event("${it.item}Type") }                        | "event: fooType\nid: fooId\n\n"
    serverSentEvent("foo") { it.event("${it.item}Type") }                                           | "event: fooType\n\n"
    serverSentEvent { it.id("fooId").event("fooType").data("fooData") }                             | "event: fooType\ndata: fooData\nid: fooId\n\n"
    serverSentEvent { it.event("fooType").data("fooData") }                                         | "event: fooType\ndata: fooData\n\n"
    serverSentEvent { it.data("fooData") }                                                          | "data: fooData\n\n"
    serverSentEvent { it.id("fooId").data("fooData") }                                              | "data: fooData\nid: fooId\n\n"
    serverSentEvent { it.id("fooId") }                                                              | "id: fooId\n\n"
    serverSentEvent { it.id("fooId").event("fooType") }                                             | "event: fooType\nid: fooId\n\n"
    serverSentEvent { it.event("fooType") }                                                         | "event: fooType\n\n"
  }

}
