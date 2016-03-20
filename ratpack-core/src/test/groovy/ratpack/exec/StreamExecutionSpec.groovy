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

package ratpack.exec

import ratpack.http.ResponseChunks
import ratpack.stream.Streams
import ratpack.stream.internal.CollectingSubscriber
import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Duration
import java.util.concurrent.TimeUnit

import static ratpack.stream.Streams.periodically

class StreamExecutionSpec extends RatpackGroovyDslSpec {

  def "stream can use promises"() {
    when:
    serverConfig { development(true) }
    handlers {
      get { ctx ->
        def s = Streams.bindExec(periodically(ctx, Duration.ofMillis(100)) { it < 10 ? it : null })
          .flatMap { n ->
          Promise.async { f ->
            ctx.get(ExecController).executor.schedule({ f.success(n) } as Runnable, 10, TimeUnit.MILLISECONDS)
          }
        }
        .map {
          it.toString()
        }

        render(ResponseChunks.stringChunks(s))
      }
    }

    then:
    text == "0123456789"
  }

  def "stream can consume stream during event promises"() {
    when:
    serverConfig { development(true) }
    handlers {
      get { ctx ->
        def s = Streams.bindExec(periodically(ctx, Duration.ofMillis(100)) { it < 10 ? it : null })
          .flatMap { n ->
          Promise.async { f ->
            def c = new CollectingSubscriber({
              f.success(it.value.get(0))
            }, { it.request(10) })

            Streams.bindExec(periodically(ctx, Duration.ofMillis(100)) {
              it < 1 ? n : null
            }).subscribe(c)
          }
        }.map { it.toString() }

        render(ResponseChunks.stringChunks(s))
      }
    }

    then:
    text == "0123456789"
  }

}
