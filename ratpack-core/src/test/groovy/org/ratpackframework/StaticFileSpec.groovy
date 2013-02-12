/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework

class StaticFileSpec extends RatpackSpec {

  def "root listing disabled"() {
    given:
    publicFile("static.text") << "hello!"
    publicFile("foo/static.text") << "hello!"

    when:
    startApp()

    then:
    urlGetConnection("").responseCode == 403
    urlGetConnection("foo").responseCode == 403
    urlGetConnection("foos").responseCode == 404
  }

  def "can serve static file"() {
    given:
    publicFile("static.text") << "hello!"

    when:
    startApp()

    then:
    urlGetText("static.text") == "hello!"
  }

  def "handlers override static files"() {
    given:
    publicFile("static.text") << "hello!"
    ratpackFile << """
      get("/") { text "foo" }
    """

    when:
    startApp()

    then:
    urlGetText("static.text") == "hello!"
    urlGetText() == "foo"

    when:
    ratpackFile << """
      get("/static.text") { text "bar" }
    """

    then:
    urlGetText("static.text") == "bar"
  }

  def "can serve index files"() {
    given:
    config.staticAssets.indexFiles << "index.xhtml"
    publicFile("index.html") << "foo"
    publicFile("dir/index.xhtml") << "bar"

    when:
    startApp()

    then:
    urlGetText() == "foo"
    urlGetText("dir") == "bar"
    urlGetText("dir/") == "bar"
  }

  def "can serve files with query strings"() {
    given:
    publicFile("index.html") << "foo"

    when:
    startApp()

    then:
    urlGetText("?abc") == "foo"
    urlGetText("index.html?abc") == "foo"
  }

}
