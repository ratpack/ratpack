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

package ratpack.hystrix

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import groovy.json.JsonSlurper
import io.netty.util.CharsetUtil
import ratpack.rx.RxRatpack
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.charset.Charset

class HystrixMetricsStreamingSpec extends RatpackGroovyDslSpec {

  def setup() {
    RxRatpack.initialize()
  }

  static class FooCommand extends HystrixCommand<String> {

    protected FooCommand() {
      super(HystrixCommandGroupKey.Factory.asKey("foo-commands"))
    }

    @Override
    protected String run() throws Exception {
      return "foo"
    }
  }

  def "can stream metrics over sse"() {
    given:
    bindings {
      add new HystrixModule().sse()
    }
    handlers {
      handler("run-command") {
        HystrixCommand<String> fooCommand = new FooCommand()
        render fooCommand.execute()
      }

      get("hystrix/sse", new HystrixMetricsEventStreamHandler())
    }

    and:
    2.times { getText("run-command") }

    expect:
    def sseEvent = rawSseResponse("/hystrix/sse")
    def metrics = new JsonSlurper().parseText(sseEvent.substring(5))
    metrics.type == "HystrixCommand"
    metrics.group == "foo-commands"
    metrics.name == "FooCommand"
    metrics.rollingCountSuccess == 2
  }

  String rawSseResponse(String path, int numberOfEvents = 1, Charset charset = CharsetUtil.UTF_8) {
    StringBuilder builder = new StringBuilder()
    Socket socket = new Socket(getAddress().host, getAddress().port)
    try {
      new OutputStreamWriter(socket.outputStream, "UTF-8").with {
        write("GET $path HTTP/1.1\r\n")
        write("\r\n")
        flush()
      }

      InputStreamReader inputStreamReader = new InputStreamReader(socket.inputStream, charset)
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader)

      def chunk
      def eventsReceived = 0
      while ((chunk = bufferedReader.readLine()) != null) {
        if (chunk.startsWith("data:")) {
          eventsReceived++
          builder.append(chunk).append("\n")
          if (eventsReceived == numberOfEvents) { break }
        }
      }

      builder.toString()
    } finally {
      socket.close()
    }
  }

}
