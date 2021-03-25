/*
 * Copyright 2017 the original author or authors.
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

package ratpack.core.http.client

import io.netty.buffer.ByteBufAllocator
import ratpack.http.client.BaseHttpClientSpec
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup

class HttpClientConnectFailureSpec extends BaseHttpClientSpec {

  @AutoCleanup
  ExecHarness exec = ExecHarness.harness()

  def "should not leak request ByteBuf on connect failure"() {
    setup:
    def requestBody = ByteBufAllocator.DEFAULT.buffer().writeBytes("test".bytes)
    def http = clientOf {}

    when:
    exec.yield { e ->
      def u = "http://localhost:20000".toURI()
      http.post(u) { spec ->
        spec.body { b ->
          b.buffer(requestBody)
        }
      }
    }

    then:
    requestBody.refCnt() == 0
  }

}
