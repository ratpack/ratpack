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

package ratpack.file

import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.multipart.FileUpload
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import ratpack.handling.Context
import ratpack.test.internal.RatpackGroovyDslSpec

class FileUploadSpec extends RatpackGroovyDslSpec {

  def "can handle file uploads"() {
    given:
    def fooFile = file("foo.txt") << "bar"

    when:
    app {
      modules {

      }
      handlers {
        post {
          render "File content: " + new String(FileUploadDecoder.parseFile(context, "theFile"), "utf8")
        }
      }
    }

    then:
    request.multiPart("theFile", fooFile)
    postText() == "File content: bar"
  }

  static class FileUploadDecoder {

    static byte[] parseFile(Context context, String name) {
      def method = HttpMethod.valueOf(context.request.method.name)
      def nettyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, context.request.uri)
      nettyRequest.headers().add(HttpHeaders.Names.CONTENT_TYPE, context.request.contentType.toString())
      def decoder = new HttpPostRequestDecoder(nettyRequest)

      def content = new DefaultHttpContent(context.request.buffer)
      decoder.offer(content)
      decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT)
      def data = decoder.getBodyHttpData(name)

      byte[] bytes

      if (data instanceof FileUpload) {
        bytes = data.get()
      } else {
        throw new Exception("$name is not a file param")
      }

      decoder.destroy()
      bytes
    }

  }

}
