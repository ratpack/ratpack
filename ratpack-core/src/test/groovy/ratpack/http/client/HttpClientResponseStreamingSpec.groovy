/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client

import io.netty.buffer.ByteBuf
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class HttpClientResponseStreamingSpec extends BaseHttpClientSpec {

  def "can cancel subscription in complete signal"() {
    when:
    otherApp {
      get {
        response.send()
      }
    }
    handlers {
      get { HttpClient http ->
        def ctx = context
        http.requestStream(otherAppUrl()) {}.then {
          it.body.bindExec().subscribe(new Subscriber<ByteBuf>() {
            Subscription s

            @Override
            void onSubscribe(Subscription s) {
              this.s = s
              s.request(Long.MAX_VALUE)
            }

            @Override
            void onNext(ByteBuf byteBuf) {
              byteBuf.release()
            }

            @Override
            void onError(Throwable t) {
              throw t
            }

            @Override
            void onComplete() {
              s.cancel()
              ctx.render "ok"
            }
          })
        }
      }
    }

    then:
    text == "ok"
  }


}
