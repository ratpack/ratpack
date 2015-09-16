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
import ratpack.site.pages.HomePage
import ratpack.site.pages.ManualPage
import ratpack.site.pages.VersionsPage
import spock.lang.Shared

class SiteBrowserSmokeSpec extends GebReportingSpec {

  @Shared
  def aut = new RatpackSiteUnderTest()

  def setup() {
    URI base = aut.address
    browser.baseUrl = base.toString()
  }

  def "go to current manual"() {
    given:
    to HomePage

    when:
    manualLink.click()

    then:
    at ManualPage
  }

  def "got to versions pages"() {
    given:
    to HomePage

    when:
    versionsLink.click()

    then:
    at VersionsPage
  }

  def cleanupSpec() {
    aut.stop()
  }

}
