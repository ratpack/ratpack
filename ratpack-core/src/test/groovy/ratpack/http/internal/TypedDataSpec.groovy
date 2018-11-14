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

import io.netty.buffer.Unpooled
import spock.lang.Specification


class TypedDataSpec extends Specification {

  def "can return byte copy from array-backed Netty buffers"() {
    given:
    def buffer = Unpooled.buffer(2000)
    buffer.writeBytes("foo".bytes)
    def typedData = new ByteBufBackedTypedData(buffer, DefaultMediaType.get("text/plain"))

    expect:
    typedData.copyBytes() == "foo".bytes
  }
}
