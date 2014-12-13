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

package ratpack.file.internal

import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll


class FileCompressionSpec extends RatpackGroovyDslSpec {

  private static final String CONTENT_ENC_HDR = "Content-Encoding"
  private static final String CONTENT_LEN_HDR = "Content-Length"
  private static final String CONTENT_TYPE_HDR = "Content-Type"
  private static final String TEST_ENCODING = "gzip"
  private static final String SMALL_CONTENT = "foo"
  private static final String MEDIUM_CONTENT = "1234567890" * 100
  private static final String LARGE_CONTENT = "1234567890" * 200
  private static final String SMALL_LEN = SMALL_CONTENT.length().toString()
  private static final String LARGE_LEN = LARGE_CONTENT.length().toString()

  def setup() {
    file "public/small.txt", SMALL_CONTENT
    file "public/medium.txt", MEDIUM_CONTENT
    file "public/large.txt", LARGE_CONTENT
    file "public/large.css", LARGE_CONTENT
    file "public/large.html", LARGE_CONTENT
    file "public/large.json", LARGE_CONTENT
    file "public/large.xml", LARGE_CONTENT
    file "public/large.gif", LARGE_CONTENT
    file "public/large.jpg", LARGE_CONTENT
    file "public/large.png", LARGE_CONTENT
    file "public/large.svg", LARGE_CONTENT
    file "public/large.mp3", LARGE_CONTENT
    file "public/large", LARGE_CONTENT

    handlers {
      assets("public")
    }
  }

  @Override
  void configureRequest(RequestSpec requestSpecification) {
    requestSpecification.headers.add("Accept-Encoding", TEST_ENCODING)
  }

  @Unroll
  def "doesn't encode when compression disabled"() {
    when:
    serverConfig {
      compressResponses(false)
    }
    def response = get(path)

    then:
    !response.headers.get(CONTENT_ENC_HDR)
    response.headers.get(CONTENT_LEN_HDR) == length

    where:
    path        | length
    "small.txt" | SMALL_LEN
    "large.txt" | LARGE_LEN
  }

  @Unroll
  def "encodes when compression enabled, larger than min size, and not an excluded content type"() {
    when:
    serverConfig {
      compressResponses(true)
    }
    def response = get(path)

    then:
    response.headers.get(CONTENT_TYPE_HDR) == type
    response.headers.get(CONTENT_ENC_HDR) == enc
    response.headers.get(CONTENT_LEN_HDR) == length

    //These fail currently because our client doesn't get the response chunked like rest assured so all lengths are set
    where:
    path         | type                       | length    | enc
//    "small.txt"  | "text/plain"               | SMALL_LEN | null
//    "large.txt"  | "text/plain"               | "53"      | TEST_ENCODING
//    "large.css"  | "text/css"                 | "53"      | TEST_ENCODING
//    "large.html" | "text/html"                | "53"      | TEST_ENCODING
//    "large.json" | "application/json"         | "53"      | TEST_ENCODING
//    "large.xml"  | "application/xml"          | "53"      | TEST_ENCODING
//    "large.gif"  | "image/gif"                | LARGE_LEN | null
//    "large.jpg"  | "image/jpeg"               | LARGE_LEN | null
    "large.png"  | "image/png"                | LARGE_LEN | null
//    "large.svg"  | "image/svg+xml"            | "53"      | TEST_ENCODING
//    "large"      | "application/octet-stream" | "53"      | TEST_ENCODING
  }

  @Unroll
  def "minimum compression size can be configured"() {
    when:
    serverConfig {
      compressResponses(true)
      compressionMinSize(minSize)
    }
    def response = get(path)

    then:
    response.headers.get(CONTENT_ENC_HDR) == enc

    where:
    path         | minSize | enc
    "small.txt"  | 0       | TEST_ENCODING
    "small.txt"  | 500     | null
    "medium.txt" | 500     | TEST_ENCODING
    "medium.txt" | 1500    | null
    "large.txt"  | 1500    | TEST_ENCODING
    "large.txt"  | 2500    | null
  }

  @Unroll
  def "images, videos, audio, archives are not compressed by default"() {
    when:
    serverConfig {
      compressResponses(true)
    }

    then:
    get(path).headers.get(CONTENT_ENC_HDR) == enc

    where:
    path        | enc
    "large.txt" | TEST_ENCODING
    "large.png" | null
    "large.jpg" | null
    "large.mp3" | null
  }

  @Unroll
  def "compression white list can be configured"() {
    when:
    serverConfig {
      compressResponses(true)
      compressionWhiteListMimeTypes("image/png")
    }

    then:
    get(path).headers.get(CONTENT_ENC_HDR) == enc

    where:
    path         | enc
    "large.txt"  | TEST_ENCODING
    "large.css"  | TEST_ENCODING
    "large.html" | TEST_ENCODING
    "large.json" | TEST_ENCODING
    "large.png"  | TEST_ENCODING
    "large"      | TEST_ENCODING
  }

  @Unroll
  def "compression black list can be configured"() {
    when:
    serverConfig {
      compressResponses(true)
      compressionBlackListMimeTypes("text/plain", "application/xml")
    }

    then:
    get(path).headers.get(CONTENT_ENC_HDR) == enc

    where:
    path         | enc
    "large.txt"  | null
    "large.css"  | TEST_ENCODING
    "large.html" | TEST_ENCODING
    "large.json" | TEST_ENCODING
    "large.xml"  | null
    "large.gif"  | TEST_ENCODING
    "large.jpg"  | TEST_ENCODING
    "large.png"  | TEST_ENCODING
    "large"      | TEST_ENCODING
  }
}
