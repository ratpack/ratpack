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

class BasicRatpackSpec extends RatpackSpec {

  def "can register route"() {
    given:
    ratpackFile << """
      get("/") {
        renderString "get"
      }
      post("/") {
        renderString "post"
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "get"
    urlPostText() == "post"
  }

  def "is reloadable"() {
    given:
    ratpackFile << """
      get("/") {
        renderString "foo"
      }
    """

    when:
    startApp()

    then:
    urlGetText() == "foo"

    when:
    ratpackFile.text = """
      get("/") {
        renderString "bar"
      }
    """

    then:
    urlGetText() == "bar"
  }

  def "can disable reloading"() {
    given:
    config.reloadRoutes false
    ratpackFile << """
      get("/") {
        renderString "foo"
      }
    """

    when:
    startApp()

    then:
    urlText() == "foo"

    when:
    ratpackFile.text = """
      get("/") {
        renderString "bar"
      }
    """

    then:
    urlText() == "foo"
  }

  def "app does not start when routes is invalid and reloading disabled"() {
    given:
    config.reloadRoutes false
    ratpackFile << """
      s s da
    """

    when:
    startApp()

    then:
    thrown(Exception)
  }

  def "can redirect"() {
    given:
    ratpackFile << """
      get("/") {
        sendRedirect "/foo"
      }
      get("/foo") {
        renderString "foo"
      }
    """

    when:
    startApp()

    then:
    urlGetText('') == "foo"
  }

}
