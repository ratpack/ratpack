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

package ratpack.site

import ratpack.groovy.test.TestHttpClient
import ratpack.groovy.test.TestHttpClients
import spock.lang.Specification

class SiteSmokeSpec extends Specification {

  def aut = new RatpackSiteUnderTest()
  @Delegate
  TestHttpClient client = TestHttpClients.testHttpClient(aut)

  def "Check Site Index"() {
    when:
    get("index.html")

    then:
    response.statusCode == 200
    response.body.contains('<title>Ratpack: A toolkit for JVM web applications</title>')

  }

  def "Check Site /"() {
    when:
    get()

    then:
    response.statusCode == 200
    response.body.contains('<title>Ratpack: A toolkit for JVM web applications</title>')
  }

  def cleanup() {
    aut.stop()
  }

}