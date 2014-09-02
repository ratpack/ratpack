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
import ratpack.sse.ServerSentEvent
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.internal.IoUtils

class ServerSentEventStreamEncoderSpec extends RatpackGroovyDslSpec {

  def encoder = new ServerSentEventsRenderer.Encoder(UnpooledByteBufAllocator.DEFAULT)

  def "can encode valid server sent events"() {
    expect:
    IoUtils.utf8String(encoder.apply(sse)) == expectedEncoding

    where:
    sse                                                                           | expectedEncoding
    ServerSentEvent.builder().id("fooId").type("fooType").data("fooData").build() | "event: fooType\ndata: fooData\nid: fooId\n\n"
    ServerSentEvent.builder().type("fooType").data("fooData").build()             | "event: fooType\ndata: fooData\n\n"
    ServerSentEvent.builder().data("fooData").build()                             | "data: fooData\n\n"
    ServerSentEvent.builder().id("fooId").data("fooData").build()                 | "data: fooData\nid: fooId\n\n"
    ServerSentEvent.builder().id("fooId").build()                                 | "id: fooId\n\n"
    ServerSentEvent.builder().id("fooId").type("fooType").build()                 | "event: fooType\nid: fooId\n\n"
    ServerSentEvent.builder().type("fooType").build()                             | "event: fooType\n\n"
  }

}
