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

class StaticRoutesFileSpec extends RatpackSpec {

  def "can use static routes file"() {
    given:
    config.routing.staticallyCompile = true

    and:
    ratpackFile << """
      get("/") {
        foo()
      }
    """

    when:
    startApp()

    then:
    errorGetText().contains("MultipleCompilationErrorsException")

    when:
    ratpackFile.text = """
      get("/") { text "foo" }
    """

    then:
    urlGetText() == "foo"
  }
}
