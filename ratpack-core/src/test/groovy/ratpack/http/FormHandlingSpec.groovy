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

import ratpack.error.ServerErrorHandler
import ratpack.error.DebugErrorHandler
import ratpack.form.Form
import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

class FormHandlingSpec extends RatpackGroovyDslSpec {

  def setup() {
    bindings {
      bind ServerErrorHandler, new DebugErrorHandler()
    }
  }

  def "can get form params"() {
    when:
    handlers {
      post {
        def form = parse Form
        render form.toString()
      }
    }

    then:
    requestSpec { it.headers.add("Content-Type", "application/x-www-form-urlencoded") }
    postText() == "[:]"
    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec ->
      //TODO Easy way to add params
      param "a", "b"
    }
    then:
    postText() == "[a:[b]]"
    when:
    resetRequest()
    requestSpec.with {
      param "a", "b", "c"
      param "d", "e"
      param "abc", ""
    }
    then:
    postText() == "[a:[b, c], d:[e], abc:[]]"
  }

  def "can read multi part forms"() {
    when:
    handlers {
      post {
        def form = parse Form
        render form.toString()
      }
    }

    and:
    requestSpec.multiPart("foo", "1", "text/plain")
    requestSpec.multiPart("bar", "2", "text/plain")
    requestSpec.multiPart("bar", "3", "text/plain")

    then:
    postText() == "[foo:[1], bar:[2, 3]]"
  }

  def "can handle file uploads"() {
    given:
    def fooFile = file "foo.txt", "bar"

    when:
    handlers {
      post {
        def form = parse Form
        render "File content: " + form.file("theFile").text
      }
    }

    then:
    requestSpec.multiPart("theFile", fooFile.toFile())
    postText() == "File content: bar"
  }

  def "default encoding is utf-8"() {
    given:
    def fooFile = file "foo.txt", "bar"

    when:
    handlers {
      post {
        def form = parse Form
        render "File type: " + form.file("theFile").contentType
      }
    }

    then:
    requestSpec.multiPart("theFile", fooFile.toFile(), "text/plain")
    postText() == "File type: text/plain;charset=UTF-8"
  }

  def "respects custom encoding"() {
    given:
    def fooFile = file "foo.txt", "bar"

    when:
    handlers {
      post {
        def form = parse Form
        render "File type: " + form.file("theFile").contentType
      }
    }

    then:
    requestSpec.multiPart("theFile", fooFile.toFile(), "text/plain;charset=US-ASCII")
    postText() == "File type: text/plain;charset=US-ASCII"
  }
}
