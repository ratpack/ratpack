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

import geb.spock.GebReportingSpec
import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.site.pages.HomePage
import ratpack.site.pages.ManualToCPage
import ratpack.site.pages.VersionsPage
import ratpack.test.ServerBackedApplicationUnderTest

class SiteBrowserSmokeSpec extends GebReportingSpec {

  private final static ServerBackedApplicationUnderTest applicationUnderTest = new LocalScriptApplicationUnderTest()

  def setup() {
    URI base = applicationUnderTest.address
    browser.baseUrl = base.toString()
  }

  def cleanup() {
    applicationUnderTest.stop()
  }

  def "go to current manual"() {
    given:
    to HomePage

    when:
    manualLink.click()

    then:
    at ManualToCPage
  }

  def "got to versions pages"() {
    given:
    to HomePage

    when:
    versionsLink.click()

    then:
    at VersionsPage
  }


}
