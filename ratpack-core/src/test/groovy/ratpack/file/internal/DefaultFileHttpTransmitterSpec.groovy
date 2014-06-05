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

import com.jayway.restassured.internal.http.ContentEncoding
import com.jayway.restassured.specification.RequestSpecification
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static com.jayway.restassured.internal.http.ContentEncoding.ACCEPT_ENC_HDR
import static com.jayway.restassured.internal.http.ContentEncoding.CONTENT_ENC_HDR

class DefaultFileHttpTransmitterSpec extends RatpackGroovyDslSpec {

  private static final String CONTENT_LEN_HDR = "Content-Length"
  private static final String CONTENT_TYPE_HDR = "Content-Type"
  private static final String TEST_ENCODING = ContentEncoding.Type.GZIP.toString()
  private static final String SMALL_CONTENT = "foo"
  private static final String LARGE_CONTENT = "1234567890" * 200
  private static final String SMALL_LEN = SMALL_CONTENT.length().toString()
  private static final String LARGE_LEN = LARGE_CONTENT.length().toString()

  def setup() {
    file "public/small.txt", SMALL_CONTENT
    file "public/large.txt", LARGE_CONTENT
    file "public/large.css", LARGE_CONTENT
    file "public/large.html", LARGE_CONTENT
    file "public/large.json", LARGE_CONTENT
    file "public/large.xml", LARGE_CONTENT
    file "public/large.gif", LARGE_CONTENT
    file "public/large.jpg", LARGE_CONTENT
    file "public/large.png", LARGE_CONTENT
    file "public/large.svg", LARGE_CONTENT
    file "public/large", LARGE_CONTENT

    handlers {
      assets("public")
    }
  }

  @Override
  void configureRequest(RequestSpecification requestSpecification) {
    requestSpecification.header(ACCEPT_ENC_HDR, TEST_ENCODING)
  }

  @Unroll
  def "doesn't encode when compression disabled"() {
    when:
    launchConfig {
      compressResponses(false)
    }
    def response = get(path)

    then:
    !response.header(CONTENT_ENC_HDR)
    response.header(CONTENT_LEN_HDR) == length

    where:
    path        | length
    "small.txt" | SMALL_LEN
    "large.txt" | LARGE_LEN
  }

  @Unroll
  def "encodes when compression enabled, larger than min size, and not an excluded content type"() {
    when:
    launchConfig {
      compressResponses(true)
    }
    def response = get(path)

    then:
    response.header(CONTENT_TYPE_HDR) == type
    response.header(CONTENT_ENC_HDR) == enc
    response.header(CONTENT_LEN_HDR) == length

    where:
    path        | type                       | length | enc
    "small.txt" | "text/plain;charset=UTF-8" | SMALL_LEN    | null
    "large.txt" | "text/plain;charset=UTF-8" | null   | TEST_ENCODING
    "large.css" | "text/css;charset=UTF-8" | null   | TEST_ENCODING
    "large.html" | "text/html;charset=UTF-8" | null   | TEST_ENCODING
    "large.json" | "application/json" | null   | TEST_ENCODING
    "large.xml" | "application/xml" | null   | TEST_ENCODING
    "large.gif" | "image/gif" | LARGE_LEN   | null
    "large.jpg" | "image/jpeg" | LARGE_LEN   | null
    "large.png" | "image/png" | LARGE_LEN   | null
    "large.svg" | "image/svg+xml" | LARGE_LEN   | null
    "large" | "application/octet-stream" | null   | TEST_ENCODING
  }
}
