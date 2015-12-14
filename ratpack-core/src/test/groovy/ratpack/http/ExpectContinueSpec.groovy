/*
 * Copyright 2015 the original author or authors.
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

import ratpack.test.internal.RatpackGroovyDslSpec

class ExpectContinueSpec extends RatpackGroovyDslSpec {

  def "can handle expect continue with no body"() {
    when:
    handlers {
      post { render "ok" }
    }

    HttpURLConnection con = applicationUnderTest.address.toURL().openConnection()
    con.setRequestMethod("POST")
    con.setRequestProperty("Expect", "100-Continue")
    con.doInput = true
    con.doOutput = false
    con.connect()

    then:
    con.inputStream.text == "ok"
  }

  def "can handle expect continue with body"() {
    when:
    handlers {
      post { render request.body.map { it.text } }
    }

    HttpURLConnection con = applicationUnderTest.address.toURL().openConnection()
    con.setRequestMethod("POST")
    con.setRequestProperty("Expect", "100-Continue")
    con.doInput = true
    con.doOutput = true
    con.connect()
    con.outputStream.withStream {
      it << "foo".getBytes("UTF-8")
    }

    then:
    con.inputStream.text == "foo"
  }

}
