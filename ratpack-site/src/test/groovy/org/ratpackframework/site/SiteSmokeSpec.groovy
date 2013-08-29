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

package org.ratpackframework.site

import org.ratpackframework.groovy.test.LocalScriptApplicationUnderTest
import org.ratpackframework.groovy.test.RequestingSupport
import org.ratpackframework.util.Action
import spock.lang.Specification

class SiteSmokeSpec extends Specification {

  def aut = new LocalScriptApplicationUnderTest()

  @Delegate RequestingSupport requestingSupport = new RequestingSupport(
    aut,
    {} as Action
  )

  def "Check Site Index"() {
    when:
    get("index.html")

    then:
    response.statusCode == 200
    response.body.asString().contains('<title>Ratpack: A toolkit for JVM web applications</title>')

  }

  def "Check Site /"() {
    when:
    get()

    then:
    response.statusCode == 200
    response.body.asString().contains('<title>Ratpack: A toolkit for JVM web applications</title>')
  }

  def cleanup() {
    aut.stop()
  }

}