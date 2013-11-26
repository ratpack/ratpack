/*
 * Copyright 2013 the original author or authors.
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

package ratpack.http

import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import ratpack.handling.Context
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.util.MultiValueMap
import ratpack.util.internal.ImmutableDelegatingMultiValueMap

class FormHandlingSpec extends RatpackGroovyDslSpec {

  def "can get form params"() {
    when:
    app {
      handlers {
        post {
          render request.form.toString()
        }
      }
    }

    then:
    postText() == "[:]" && resetRequest()
    request.with {
      param "a", "b"
    }
    postText() == "[a:[b]]" && resetRequest()
    request.with {
      param "a", "b", "c"
      param "d", "e"
      param "abc"
    }
    postText() == "[a:[b, c], d:[e], abc:[]]" && resetRequest()
  }

  def "can read multi part forms"() {
    when:
    app {
      handlers {
        post {
          render FormDecoder.parseForm(context).toString()
        }
      }
    }

    and:
    request.multiPart("foo", "1", "text/plain")
    request.multiPart("bar", "2", "text/plain")

    then:
    postText() == "[foo:[1], bar:[2]]"
  }

  static class FormDecoder {

    static MultiValueMap<String, String> parseForm(Context context) {
      def method = io.netty.handler.codec.http.HttpMethod.valueOf(context.request.method.name)
      def nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, context.request.uri)
      nettyRequest.headers().add(HttpHeaders.Names.CONTENT_TYPE, context.request.contentType.toString())
      def decoder = new HttpPostRequestDecoder(nettyRequest)

      def content = new DefaultHttpContent(context.request.buffer)
      decoder.offer(content)
      decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT)

      Map<String, List<String>> backingMap = [:].withDefault { new ArrayList<>(1) }

      InterfaceHttpData data
      try {
        while (data = decoder.next()) {
          if (data.httpDataType == InterfaceHttpData.HttpDataType.Attribute) {
            backingMap.get(data.name) << ((Attribute) data).value
          }
          data.release()
        }
      } catch (EndOfDataDecoderException ignore) {
      }

      decoder.destroy()

      new ImmutableDelegatingMultiValueMap<String, String>(backingMap)
    }

  }
}
