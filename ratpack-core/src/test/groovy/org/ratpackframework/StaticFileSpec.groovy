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

import org.ratpackframework.test.DefaultRatpackSpec

class StaticFileSpec extends DefaultRatpackSpec {

  def "root listing disabled"() {
    given:
    assetFile("static.text") << "hello!"
    assetFile("foo/static.text") << "hello!"

    when:
    startApp()

    then:
    urlGetConnection("").responseCode == 403
    urlGetConnection("foo").responseCode == 403
    urlGetConnection("foos").responseCode == 404
  }

  def "can serve static file"() {
    given:
    assetFile("static.text") << "hello!"

    when:
    startApp()

    then:
    urlGetText("static.text") == "hello!"
  }

  def "handlers override static files"() {
    given:
    assetFile("static.text") << "hello!"
    routing {
      get("/") { text "foo" }
    }

    when:
    startApp()

    then:
    urlGetText("static.text") == "hello!"
    urlGetText() == "foo"

    when:
    routing {
      get("/static.text") { text "bar" }
    }
    restartApp()

    then:
    urlGetText("static.text") == "bar"
  }

  def "can serve index files"() {
    given:
    staticAssets.indexFiles << "index.xhtml"
    assetFile("index.html") << "foo"
    assetFile("dir/index.xhtml") << "bar"

    when:
    startApp()

    then:
    urlGetText() == "foo"
    urlGetText("dir") == "bar"
    urlGetText("dir/") == "bar"
  }

  def "can serve files with query strings"() {
    given:
    assetFile("index.html") << "foo"

    when:
    startApp()

    then:
    urlGetText("?abc") == "foo"
    urlGetText("index.html?abc") == "foo"
  }

}
